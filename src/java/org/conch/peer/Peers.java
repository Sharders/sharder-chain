/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.peer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.conch.Account;
import org.conch.Block;
import org.conch.Constants;
import org.conch.Db;
import org.conch.Conch;
import org.conch.Transaction;
import org.conch.http.API;
import org.conch.http.APIEnum;
import org.conch.util.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Peers {

    public enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER, ADD_INBOUND, REMOVE_INBOUND, CHANGED_SERVICES
    }

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static volatile int communicationLoggingMask;

    private static final List<String> wellKnownPeers;
    static final Set<String> knownBlacklistedPeers;

    static final int connectTimeout;
    static final int readTimeout;
    static final int blacklistingPeriod;
    static final boolean getMorePeers;
    static final int MAX_REQUEST_SIZE = 20 * 1024 * 1024;
    static final int MAX_RESPONSE_SIZE = 20 * 1024 * 1024;
    static final int MAX_MESSAGE_SIZE = 20 * 1024 * 1024;
    public static final int MIN_COMPRESS_SIZE = 256;
    static final boolean useWebSockets;
    static final int webSocketIdleTimeout;

    public static String getMyAddress() {
        return myAddress;
    }

    public static boolean isUseNATService() {
        return useNATService;
    }

    public static boolean isMyAddressAnnounced() {
        return myAddress != null;
    }

    // [NAT] useNATService and client configuration
    static boolean useNATService = Conch.getBooleanProperty("sharder.useNATService");
    static final String NATServiceAddress = Convert.emptyToNull(Conch.getStringProperty("sharder.NATServiceAddress"));
    static final int NATServicePort = Conch.getIntProperty("sharder.NATServicePort");
    static final String NATClientKey = Convert.emptyToNull(Conch.getStringProperty("sharder.NATClientKey"));

    static final boolean useProxy = System.getProperty("socksProxyHost") != null || System.getProperty("http.proxyHost") != null;
    static final boolean isGzipEnabled;

    private static final int DEFAULT_PEER_PORT = 3218;
    private static final int TESTNET_PEER_PORT = 8218;
    private static final int DEVNET_PEER_PORT = 9218;
    private static final String myPlatform;
    private static final String myAddress;
    private static final int configuredServerPort;
    private static final String myHallmark;
    private static final boolean shareMyAddress;
    private static final boolean enablePeerUPnP;
    private static final int maxNumberOfInboundConnections;
    private static final int maxNumberOfOutboundConnections;
    public static final int maxNumberOfConnectedPublicPeers;
    private static final int maxNumberOfKnownPeers;
    private static final int minNumberOfKnownPeers;
    private static final boolean enableHallmarkProtection;
    private static final int pushThreshold;
    private static final int pullThreshold;
    private static final int sendToPeersLimit;
    private static final boolean usePeersDb;
    private static final boolean savePeers;
    static final boolean ignorePeerAnnouncedAddress;
    static final boolean cjdnsOnly;
    static final int MAX_VERSION_LENGTH = 10;
    static final int MAX_APPLICATION_LENGTH = 20;
    static final int MAX_PLATFORM_LENGTH = 30;
    static final int MAX_ANNOUNCED_ADDRESS_LENGTH = 100;
    static final boolean hideErrorDetails = Conch.getBooleanProperty("sharder.hideErrorDetails");
    private static String bestPeer = "127.0.0.1";
    private static PeerLoad myLoad;

    private static final JSONObject myPeerInfo;
    private static final List<Peer.Service> myServices;
    private static volatile Peer.BlockchainState currentBlockchainState;
    private static volatile JSONStreamAware myPeerInfoRequest;
    private static volatile JSONStreamAware myPeerInfoResponse;

    private static final Listeners<Peer,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> selfAnnouncedAddresses = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    static final ExecutorService peersService = new QueuedThreadPool(2, 15);
    private static final ExecutorService sendingService = Executors.newFixedThreadPool(10);

    private static final boolean enableBizAPIs = Conch.getBooleanProperty("sharder.enableBizAPIs");
    private static final boolean enableStorage = Conch.getBooleanProperty("sharder.enableStorage");

    private static List<String> loadPeersSetting(){
        if(Constants.isDevnet()) {
            List<String> devPeers = new ArrayList<>();
            // Devnet bootstarp node
            devPeers.add("192.168.31.100");
            return devPeers;
        }

        return Constants.isTestnet() ? Conch.getStringListProperty("sharder.defaultTestnetPeers") : Conch.getStringListProperty("sharder.defaultPeers");
    }

    static {

        String platform = Conch.getStringProperty("sharder.myPlatform", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        if (platform.length() > MAX_PLATFORM_LENGTH) {
            platform = platform.substring(0, MAX_PLATFORM_LENGTH);
        }
        myPlatform = platform;
        myAddress = Convert.emptyToNull(Conch.getStringProperty("sharder.myAddress", "").trim());
        if (myAddress != null && myAddress.endsWith(":" + TESTNET_PEER_PORT) && !Constants.isTestnet()) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }

        String myHost = null;
        int myPort = -1;
        if (myAddress != null) {
            try {
                URI uri = new URI("http://" + myAddress);
                myHost = uri.getHost();
                myPort = (uri.getPort() == -1 ? Peers.getDefaultPeerPort() : uri.getPort());
                boolean addrValid = false;
                // [NAT] NAT Service check
                if (useNATService) {
                    Logger.logInfoMessage("Node joins the network via sharder official or 3rd part NAT|DDNS service");
//                    if (NetStateUtil.isReachable(myHost)) {
//                        Logger.logInfoMessage("NAT service [" + myHost +"] can be connected");
//                        addrValid = true;
//                    } else {
//                        Logger.logErrorMessage("Node now trying to join the network via NAT service, but NAT service [" + myHost +"] can not be reached now");
//                    }
                }
                InetAddress[] myAddrs = InetAddress.getAllByName(myHost);
                Enumeration<NetworkInterface> intfs = NetworkInterface.getNetworkInterfaces();
                chkAddr: while (intfs.hasMoreElements()) {
                    NetworkInterface intf = intfs.nextElement();
                    List<InterfaceAddress> intfAddrs = intf.getInterfaceAddresses();
                    for (InterfaceAddress intfAddr: intfAddrs) {
                        InetAddress extAddr = intfAddr.getAddress();
                        for (InetAddress myAddr : myAddrs) {
                            if (extAddr.equals(myAddr)) {
                                addrValid = true;
                                break chkAddr;
                            }
                        }
                    }
                }
                if (!addrValid) {
                    InetAddress extAddr = UPnP.getExternalAddress();
                    if (extAddr != null) {
                        for (InetAddress myAddr : myAddrs) {
                            if (extAddr.equals(myAddr)) {
                                addrValid = true;
                                break;
                            }
                        }
                    }
                }
                if (!addrValid) {
                    Logger.logWarningMessage("Your announced address does not match your external address");
                }
            } catch (SocketException e) {
                Logger.logErrorMessage("Unable to enumerate the network interfaces :" + e.toString());
            } catch (URISyntaxException | UnknownHostException e) {
                Logger.logWarningMessage("Your announced address is not valid: " + e.toString());
            }
        }
        configuredServerPort = Conch.getIntProperty("sharder.peerServerPort");
        checkNetworkWhetherRight(myHost, getPort(), true);
        shareMyAddress = Conch.getBooleanProperty("sharder.shareMyAddress") && ! Constants.isOffline;
        enablePeerUPnP = Conch.getBooleanProperty("sharder.enablePeerUPnP");
        myHallmark = Convert.emptyToNull(Conch.getStringProperty("sharder.myHallmark", "").trim());
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (!hallmark.isValid()) {
                    throw new RuntimeException("Hallmark is not valid");
                }
                if (myAddress != null) {
                    if (!hallmark.getHost().equals(myHost)) {
                        throw new RuntimeException("Invalid hallmark host");
                    }
                    if (myPort != hallmark.getPort()) {
                        throw new RuntimeException("Invalid hallmark port");
                    }
                }
            } catch (RuntimeException e) {
                Logger.logErrorMessage("Your hallmark is invalid: " + Peers.myHallmark + " for your address: " + myAddress + "[" + e.getMessage() + "]");
                throw new RuntimeException(e.toString(), e);
            }
        }
        List<Peer.Service> servicesList = new ArrayList<>();
        JSONObject json = new JSONObject();
        if (myAddress != null) {
            try {
                URI uri = new URI("http://" + myAddress);
                String host = uri.getHost();
                int port = uri.getPort();
                String announcedAddress;
                if (!Constants.isTestnet()) {
                    if (port >= 0)
                        announcedAddress = myAddress;
                    else
                        announcedAddress = host + (configuredServerPort != DEFAULT_PEER_PORT ? ":" + configuredServerPort : "");
                } else {
                    //[NAT] Lanproxy use serverIP+port(for client),so use host+port = myAddress
                    announcedAddress = myAddress;
                }
                if (announcedAddress == null || announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
                    throw new RuntimeException("Invalid announced address length: " + announcedAddress);
                }
                json.put("announcedAddress", announcedAddress);
            } catch (URISyntaxException e) {
                Logger.logMessage("Your announce address is invalid: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            json.put("hallmark", Peers.myHallmark);
            servicesList.add(Peer.Service.HALLMARK);
        }
        json.put("useNATService", useNATService);
        json.put("application", Conch.APPLICATION);
        json.put("version", Conch.VERSION);
        json.put("platform", Peers.myPlatform);
        json.put("shareAddress", Peers.shareMyAddress);
        if (!Constants.ENABLE_PRUNING && Constants.INCLUDE_EXPIRED_PRUNABLE) {
            servicesList.add(Peer.Service.PRUNABLE);
        }
        if (API.openAPIPort > 0) {
            json.put("apiPort", API.openAPIPort);
            servicesList.add(Peer.Service.API);
        }
        if (API.openAPISSLPort > 0) {
            json.put("apiSSLPort", API.openAPISSLPort);
            servicesList.add(Peer.Service.API_SSL);
        }

        if (API.openAPIPort > 0 || API.openAPISSLPort > 0) {
            EnumSet<APIEnum> disabledAPISet = EnumSet.noneOf(APIEnum.class);

            API.disabledAPIs.forEach(apiName -> {
                APIEnum api = APIEnum.fromName(apiName);
                if (api != null) {
                    disabledAPISet.add(api);
                }
            });
            API.disabledAPITags.forEach(apiTag -> {
                for (APIEnum api : APIEnum.values()) {
                    if (api.getHandler() != null && api.getHandler().getAPITags().contains(apiTag)) {
                        disabledAPISet.add(api);
                    }
                }
            });
            json.put("disabledAPIs", APIEnum.enumSetToBase64String(disabledAPISet));

            json.put("apiServerIdleTimeout", API.apiServerIdleTimeout);

            if (API.apiServerCORS) {
                servicesList.add(Peer.Service.CORS);
            }
        }

        // Add Business API service
        if (enableBizAPIs) {
            json.put("enableBizAPIs", true);
            servicesList.add(Peer.Service.BAPI);
        }

        // Add Storage service
        if (enableStorage) {
            json.put("enableStorage", true);
            servicesList.add(Peer.Service.STORAGE);
        }

        long services = 0;
        for (Peer.Service service : servicesList) {
            services |= service.getCode();
        }
        json.put("services", Long.toUnsignedString(services));
        myServices = Collections.unmodifiableList(servicesList);
        Logger.logDebugMessage("My peer info:\n" + json.toJSONString());

        myPeerInfo = json;

        myLoad = new PeerLoad("127.0.0.1",API.openAPIPort,-1);


        final List<String> defaultPeers = loadPeersSetting();

        wellKnownPeers = Collections.unmodifiableList(Constants.isTestnet() ? Conch.getStringListProperty("sharder.testnetPeers")
                : Conch.getStringListProperty("sharder.wellKnownPeers"));

        List<String> knownBlacklistedPeersList = Conch.getStringListProperty("sharder.knownBlacklistedPeers");
        if (knownBlacklistedPeersList.isEmpty()) {
            knownBlacklistedPeers = Collections.emptySet();
        } else {
            knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
        }

        maxNumberOfInboundConnections = Conch.getIntProperty("sharder.maxNumberOfInboundConnections");
        maxNumberOfOutboundConnections = Conch.getIntProperty("sharder.maxNumberOfOutboundConnections");
        maxNumberOfConnectedPublicPeers = Math.min(Conch.getIntProperty("sharder.maxNumberOfConnectedPublicPeers"),
                maxNumberOfOutboundConnections);
        maxNumberOfKnownPeers = Conch.getIntProperty("sharder.maxNumberOfKnownPeers");
        minNumberOfKnownPeers = Conch.getIntProperty("sharder.minNumberOfKnownPeers");
        connectTimeout = Conch.getIntProperty("sharder.connectTimeout");
        readTimeout = Conch.getIntProperty("sharder.readTimeout");
        enableHallmarkProtection = Conch.getBooleanProperty("sharder.enableHallmarkProtection") && !Constants.isLightClient;
        pushThreshold = Conch.getIntProperty("sharder.pushThreshold");
        pullThreshold = Conch.getIntProperty("sharder.pullThreshold");
        useWebSockets = Conch.getBooleanProperty("sharder.useWebSockets");
        webSocketIdleTimeout = Conch.getIntProperty("sharder.webSocketIdleTimeout");
        isGzipEnabled = Conch.getBooleanProperty("sharder.enablePeerServerGZIPFilter");
        blacklistingPeriod = Conch.getIntProperty("sharder.blacklistingPeriod") / 1000;
        communicationLoggingMask = Conch.getIntProperty("sharder.communicationLoggingMask");
        sendToPeersLimit = Conch.getIntProperty("sharder.sendToPeersLimit");
        usePeersDb = Conch.getBooleanProperty("sharder.usePeersDb") && ! Constants.isOffline;
        savePeers = usePeersDb && Conch.getBooleanProperty("sharder.savePeers");
        getMorePeers = Conch.getBooleanProperty("sharder.getMorePeers");
        cjdnsOnly = Conch.getBooleanProperty("sharder.cjdnsOnly");
        ignorePeerAnnouncedAddress = Conch.getBooleanProperty("sharder.ignorePeerAnnouncedAddress");
        if (useWebSockets && useProxy) {
            Logger.logMessage("Using a proxy, will not create outbound websockets.");
        }

        final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<>());

        if (!Constants.isOffline) {
            ThreadPool.runBeforeStart(new Runnable() {

                private final Set<PeerDb.Entry> entries = new HashSet<>();

                @Override
                public void run() {
                    final int now = Conch.getEpochTime();
                    wellKnownPeers.forEach(address -> entries.add(new PeerDb.Entry(address, 0, now)));
                    if (usePeersDb) {
                        Logger.logDebugMessage("Loading known peers from the database...");
                        defaultPeers.forEach(address -> entries.add(new PeerDb.Entry(address, 0, now)));
                        if (savePeers) {
                            List<PeerDb.Entry> dbPeers = PeerDb.loadPeers();
                            dbPeers.forEach(entry -> {
                                if (!entries.add(entry)) {
                                    // Database entries override entries from sharder.properties
                                    entries.remove(entry);
                                    entries.add(entry);
                                }
                            });
                        }
                    }
                    entries.forEach(entry -> {
                        Future<String> unresolvedAddress = peersService.submit(() -> {
                            PeerImpl peer = Peers.findOrCreatePeer(entry.getAddress(), Peers.isUseNATService(entry.getAddress()));
                            if (peer != null) {
                                peer.setLastUpdated(entry.getLastUpdated());
                                peer.setServices(entry.getServices());
                                Peers.addPeer(peer);
                                return null;
                            }
                            return entry.getAddress();
                        });
                        unresolvedPeers.add(unresolvedAddress);
                    });
                }
            }, false);
        }

        ThreadPool.runAfterStart(() -> {
            for (Future<String> unresolvedPeer : unresolvedPeers) {
                try {
                    String badAddress = unresolvedPeer.get(5, TimeUnit.SECONDS);
                    if (badAddress != null) {
                        Logger.logDebugMessage("Failed to resolve peer address: " + badAddress);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Logger.logDebugMessage("Failed to add peer", e);
                } catch (TimeoutException ignore) {
                }
            }
            Logger.logDebugMessage("Known peers: " + peers.size());
        });

    }

    private static final int getPort() {
        return Constants.isDevnet() ? DEVNET_PEER_PORT : (Constants.isTestnet() ? TESTNET_PEER_PORT : Peers.configuredServerPort);
    }


    private static class Init {

        private final static Server peerServer;

        static {
            if (Peers.shareMyAddress) {
                peerServer = new Server();
                ServerConnector connector = new ServerConnector(peerServer);
                final int port = getPort();
                connector.setPort(port);
                final String host = Conch.getStringProperty("sharder.peerServerHost");
                connector.setHost(host);
                connector.setIdleTimeout(Conch.getIntProperty("sharder.peerServerIdleTimeout"));
                connector.setReuseAddress(true);
                peerServer.addConnector(connector);
                ServletContextHandler ctxHandler = new ServletContextHandler();
                ctxHandler.setContextPath("/");

                ServletHolder peerServletHolder = new ServletHolder(new PeerServlet());
                ctxHandler.addServlet(peerServletHolder, "/*");

                if (Conch.getBooleanProperty("sharder.enablePeerServerDoSFilter")) {
                    FilterHolder dosFilterHolder = ctxHandler.addFilter(DoSFilter.class, "/*",
                            EnumSet.of(DispatcherType.REQUEST));
                    dosFilterHolder.setInitParameter("maxRequestsPerSec", Conch.getStringProperty("sharder.peerServerDoSFilter.maxRequestsPerSec"));
                    dosFilterHolder.setInitParameter("delayMs", Conch.getStringProperty("sharder.peerServerDoSFilter.delayMs"));
                    dosFilterHolder.setInitParameter("maxRequestMs", Conch.getStringProperty("sharder.peerServerDoSFilter.maxRequestMs"));
                    dosFilterHolder.setInitParameter("trackSessions", "false");
                    dosFilterHolder.setAsyncSupported(true);
                }

                if (isGzipEnabled) {
                    GzipHandler gzipHandler = new GzipHandler();
                    gzipHandler.setIncludedMethods("GET", "POST");
                    gzipHandler.setIncludedPaths("/*");
                    gzipHandler.setMinGzipSize(MIN_COMPRESS_SIZE);
                    ctxHandler.setGzipHandler(gzipHandler);
                }

                peerServer.setHandler(ctxHandler);
                peerServer.setStopAtShutdown(true);
                // Set maxFormContentSize to -1 to unlimited Form size to support large data upload from http
                peerServer.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", -1);
                ThreadPool.runBeforeStart(() -> {
                    try {
                        if (enablePeerUPnP) {
                            Connector[] peerConnectors = peerServer.getConnectors();
                            for (Connector peerConnector : peerConnectors) {
                                if (peerConnector instanceof ServerConnector)
                                    UPnP.addPort(((ServerConnector)peerConnector).getPort());
                            }
                        }
                        peerServer.start();
                        Logger.logMessage("Started peer networking server at " + host + ":" + port);
                    } catch (Exception e) {
                        Logger.logErrorMessage("Failed to start peer networking server", e);
                        throw new RuntimeException(e.toString(), e);
                    }
                }, true);
            } else {
                peerServer = null;
                Logger.logMessage("shareMyAddress is disabled, will not start peer networking server");
            }

            //[NAT] Run NAT client command
            if (useNATService) {
                StringBuilder cmd = new StringBuilder(SystemUtils.IS_OS_WINDOWS ? "nat_client.exe" : "./nat_client");
                cmd.append(" -s ").append(NATServiceAddress == null?Peers.addressHost(myAddress):NATServiceAddress)
                        .append(" -p ").append(NATServicePort)
                        .append(" -k ").append(NATClientKey);
                try {
                    Process process = Runtime.getRuntime().exec(cmd.toString());
                    // any error message?
                    StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR");
                    // any output?
                    StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");
                    // kick them off
                    errorGobbler.start();
                    outputGobbler.start();
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroy()));
                    Logger.logInfoMessage("NAT Client execute: " + cmd.toString());
                } catch (IOException e) {
                    useNATService = false;
                    Logger.logErrorMessage("NAT Client execute Error", e);
                }
            }
        }

        private static void init() {}

        private Init() {}

    }

    private static final Runnable peerUnBlacklistingThread = () -> {

        try {
            try {

                int curTime = Conch.getEpochTime();
                for (PeerImpl peer : peers.values()) {
                    peer.updateBlacklistedStatus(curTime);
                }

            } catch (Exception e) {
                Logger.logDebugMessage("Error un-blacklisting peer", e);
            }
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
            System.exit(1);
        }

    };

    private static final Runnable peerConnectingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    final int now = Conch.getEpochTime();
                    if (!hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                        List<Future<?>> futures = new ArrayList<>();
                        List<Peer> hallmarkedPeers = getPeers(peer -> !peer.isBlacklisted()
                                && peer.getAnnouncedAddress() != null
                                && peer.getState() != Peer.State.CONNECTED
                                && now - peer.getLastConnectAttempt() > 600
                                && peer.providesService(Peer.Service.HALLMARK));
                        List<Peer> nonhallmarkedPeers = getPeers(peer -> !peer.isBlacklisted()
                                && peer.getAnnouncedAddress() != null
                                && peer.getState() != Peer.State.CONNECTED
                                && now - peer.getLastConnectAttempt() > 600
                                && !peer.providesService(Peer.Service.HALLMARK));
                        if (!hallmarkedPeers.isEmpty() || !nonhallmarkedPeers.isEmpty()) {
                            Set<PeerImpl> connectSet = new HashSet<>();
                            for (int i = 0; i < 10; i++) {
                                List<Peer> peerList;
                                if (hallmarkedPeers.isEmpty()) {
                                    peerList = nonhallmarkedPeers;
                                } else if (nonhallmarkedPeers.isEmpty()) {
                                    peerList = hallmarkedPeers;
                                } else {
                                    peerList = (ThreadLocalRandom.current().nextInt(2) == 0 ? hallmarkedPeers : nonhallmarkedPeers);
                                }
                                connectSet.add((PeerImpl)peerList.get(ThreadLocalRandom.current().nextInt(peerList.size())));
                            }
                            connectSet.forEach(peer -> futures.add(peersService.submit(() -> {
                                peer.connect();
                                if (peer.getState() == Peer.State.CONNECTED &&
                                            enableHallmarkProtection && peer.getWeight() == 0 &&
                                            hasTooManyOutboundConnections()) {
                                    Logger.logDebugMessage("Too many outbound connections, deactivating peer " + peer.getHost());
                                    peer.deactivate();
                                }
                                return null;
                            })));
                            for (Future<?> future : futures) {
                                future.get();
                            }
                        }
                    }

                    peers.values().forEach(peer -> {
                        if (peer.getState() == Peer.State.CONNECTED
                                && now - peer.getLastUpdated() > 3600
                                && now - peer.getLastConnectAttempt() > 600) {
                            peersService.submit(peer::connect);
                        }
                        if (peer.getLastInboundRequest() != 0 &&
                                now - peer.getLastInboundRequest() > Peers.webSocketIdleTimeout / 1000) {
                            peer.setLastInboundRequest(0);
                            notifyListeners(peer, Event.REMOVE_INBOUND);
                        }
                    });

                    if (hasTooManyKnownPeers() && hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                        int initialSize = peers.size();
                        for (PeerImpl peer : peers.values()) {
                            if (now - peer.getLastUpdated() > 24 * 3600) {
                                peer.remove();
                            }
                            if (hasTooFewKnownPeers()) {
                                break;
                            }
                        }
                        if (hasTooManyKnownPeers()) {
                            PriorityQueue<PeerImpl> sortedPeers = new PriorityQueue<>(peers.values());
                            int skipped = 0;
                            while (skipped < Peers.minNumberOfKnownPeers) {
                                if (sortedPeers.poll() == null) {
                                    break;
                                }
                                skipped += 1;
                            }
                            while (!sortedPeers.isEmpty()) {
                                sortedPeers.poll().remove();
                            }
                        }
                        Logger.logDebugMessage("Reduced peer pool size from " + initialSize + " to " + peers.size());
                    }

                    for (String wellKnownPeer : wellKnownPeers) {
                        PeerImpl peer = findOrCreatePeer(wellKnownPeer, Peers.isUseNATService(wellKnownPeer));
                        if (peer != null && now - peer.getLastUpdated() > 3600 && now - peer.getLastConnectAttempt() > 600) {
                            peersService.submit(() -> {
                                addPeer(peer);
                                connectPeer(peer);
                            });
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error connecting to peer", e);
                }
                //current peer is best peer
                if(!"127.0.0.1".equals(bestPeer) && myServices.contains(Peer.Service.BAPI)
                        && getMyPeerLoad().getLoad() < peers.get(bestPeer).getPeerLoad().getLoad()){
                    bestPeer = "127.0.0.1";
                }
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
                System.exit(1);
            }

        }

    };

    private static final Runnable getMorePeersThread = new Runnable() {

        private final JSONStreamAware getPeersRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getPeers");
            getPeersRequest = JSON.prepareRequest(request);
        }

        private volatile boolean updatedPeer;

        @Override
        public void run() {

            try {
                try {
                    if (hasTooManyKnownPeers()) {
                        return;
                    }
                    Peer peer = getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    //[NAT] inject useNATService property to the request params
                    JSONObject request = new JSONObject();
                    request.put("requestType", "getPeers");
                    request.put("useNATService", Peers.isUseNATService());
                    request.put("announcedAddress", Peers.getMyAddress());
                    JSONObject response = peer.send(JSON.prepareRequest(request), Constants.MAX_RESPONSE_SIZE);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray)response.get("peers");
                    Set<String> addedAddresses = new HashSet<>();
                    if (peers != null) {
                        JSONArray services = (JSONArray)response.get("services");
                        boolean setServices = (services != null && services.size() == peers.size());
                        int now = Conch.getEpochTime();
                        for (int i=0; i<peers.size(); i++) {
                            String announcedAddress = (String)peers.get(i);
                            PeerImpl newPeer = findOrCreatePeer(announcedAddress, Peers.isUseNATService(announcedAddress));
                            if (newPeer != null) {
                                if (now - newPeer.getLastUpdated() > 24 * 3600) {
                                    newPeer.setLastUpdated(now);
                                    updatedPeer = true;
                                }
                                if (Peers.addPeer(newPeer) && setServices) {
                                    newPeer.setServices(Long.parseUnsignedLong((String)services.get(i)));
                                }
                                addedAddresses.add(announcedAddress);
                                if (hasTooManyKnownPeers()) {
                                    break;
                                }
                            }
                        }
                        if (savePeers && updatedPeer) {
                            updateSavedPeers();
                            updatedPeer = false;
                        }
                    }

                    JSONArray myPeers = new JSONArray();
                    JSONArray myServices = new JSONArray();
                    Peers.getAllPeers().forEach(myPeer -> {
                        if (!myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null
                                && myPeer.getState() == Peer.State.CONNECTED && myPeer.shareAddress()
                                && !addedAddresses.contains(myPeer.getAnnouncedAddress())
                                && !myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())) {
                            myPeers.add(myPeer.getAnnouncedAddress());
                            myServices.add(Long.toUnsignedString(((PeerImpl) myPeer).getServices()));
                        }
                    });
                    if (myPeers.size() > 0) {
                        //[NAT] inject useNATService property to the request params
                        request.clear();
                        request.put("requestType", "addPeers");
                        request.put("useNATService", Peers.isUseNATService());
                        request.put("announcedAddress", Peers.getMyAddress());
                        request.put("peers", myPeers);
                        request.put("services", myServices);            // Separate array for backwards compatibility
                        peer.send(JSON.prepareRequest(request), 0);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error requesting peers from a peer", e);
                }
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
                System.exit(1);
            }

        }

        private void updateSavedPeers() {
            int now = Conch.getEpochTime();
            //
            // Load the current database entries and map announced address to database entry
            //
            List<PeerDb.Entry> oldPeers = PeerDb.loadPeers();
            Map<String, PeerDb.Entry> oldMap = new HashMap<>(oldPeers.size());
            oldPeers.forEach(entry -> oldMap.put(entry.getAddress(), entry));
            //
            // Create the current peer map (note that there can be duplicate peer entries with
            // the same announced address)
            //
            Map<String, PeerDb.Entry> currentPeers = new HashMap<>();
            Peers.peers.values().forEach(peer -> {
                if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted() && now - peer.getLastUpdated() < 7*24*3600) {
                    currentPeers.put(peer.getAnnouncedAddress(),
                            new PeerDb.Entry(peer.getAnnouncedAddress(), peer.getServices(), peer.getLastUpdated()));
                }
            });
            //
            // Build toDelete and toUpdate lists
            //
            List<PeerDb.Entry> toDelete = new ArrayList<>(oldPeers.size());
            oldPeers.forEach(entry -> {
                if (currentPeers.get(entry.getAddress()) == null)
                    toDelete.add(entry);
            });
            List<PeerDb.Entry> toUpdate = new ArrayList<>(currentPeers.size());
            currentPeers.values().forEach(entry -> {
                PeerDb.Entry oldEntry = oldMap.get(entry.getAddress());
                if (oldEntry == null || entry.getLastUpdated() - oldEntry.getLastUpdated() > 24*3600)
                    toUpdate.add(entry);
            });
            //
            // Nothing to do if all of the lists are empty
            //
            if (toDelete.isEmpty() && toUpdate.isEmpty())
                return;
            //
            // Update the peer database
            //
            try {
                Db.db.beginTransaction();
                PeerDb.deletePeers(toDelete);
                PeerDb.updatePeers(toUpdate);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        }

    };

    static {
        Peers.addListener(peer -> peersService.submit(() -> {
            if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted()) {
                try {
                    Db.db.beginTransaction();
                    PeerDb.updatePeer((PeerImpl)peer);
                    Db.db.commitTransaction();
                } catch (RuntimeException e) {
                    Logger.logErrorMessage("Unable to update peer database", e);
                    Db.db.rollbackTransaction();
                } finally {
                    Db.db.endTransaction();
                }
            }
        }), Peers.Event.CHANGED_SERVICES);
    }

    static {
        Account.addListener(account -> peers.values().forEach(peer -> {
            if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                Peers.listeners.notify(peer, Event.WEIGHT);
            }
        }), Account.Event.BALANCE);
    }

    static {
        if (! Constants.isOffline) {
            ThreadPool.scheduleThread("PeerConnecting", Peers.peerConnectingThread, 20);
            ThreadPool.scheduleThread("PeerUnBlacklisting", Peers.peerUnBlacklistingThread, 60);
            if (Peers.getMorePeers) {
                ThreadPool.scheduleThread("GetMorePeers", Peers.getMorePeersThread, 20);
            }
        }
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        if (Init.peerServer != null) {
            try {
                Init.peerServer.stop();
                if (enablePeerUPnP) {
                    Connector[] peerConnectors = Init.peerServer.getConnectors();
                    for (Connector peerConnector : peerConnectors) {
                        if (peerConnector instanceof ServerConnector)
                            UPnP.deletePort(((ServerConnector)peerConnector).getPort());
                    }
                }
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to stop peer server", e);
            }
        }
        ThreadPool.shutdownExecutor("sendingService", sendingService, 2);
        ThreadPool.shutdownExecutor("peersService", peersService, 5);
    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static int getDefaultPeerPort() {
        return Constants.isDevnet() ? DEVNET_PEER_PORT : (Constants.isTestnet() ? TESTNET_PEER_PORT : DEFAULT_PEER_PORT);
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static List<Peer> getActivePeers() {
        return getPeers(peer -> peer.getState() != Peer.State.NON_CONNECTED);
    }

    public static List<Peer> getPeers(final Peer.State state) {
        return getPeers(peer -> peer.getState() == state);
    }

    public static List<Peer> getPeers(Filter<Peer> filter) {
        return getPeers(filter, Integer.MAX_VALUE);
    }

    public static List<Peer> getPeers(Filter<Peer> filter, int limit) {
        List<Peer> result = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (filter.ok(peer)) {
                result.add(peer);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    public static Peer getPeer(String host) {
        return peers.get(host);
    }

    public static List<Peer> getInboundPeers() {
        return getPeers(Peer::isInbound);
    }

    public static boolean hasTooManyInboundPeers() {
        return getPeers(Peer::isInbound, maxNumberOfInboundConnections).size() >= maxNumberOfInboundConnections;
    }

    public static boolean hasTooManyOutboundConnections() {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null,
                maxNumberOfOutboundConnections).size() >= maxNumberOfOutboundConnections;
    }

    public static PeerImpl findOrCreatePeer(String announcedAddress, boolean useNATService, boolean create) {
        if (announcedAddress == null) {
            return null;
        }
        announcedAddress = announcedAddress.trim().toLowerCase();
        PeerImpl peer;
        if ((peer = peers.get(announcedAddress)) != null) {
            return peer;
        }
        String host = selfAnnouncedAddresses.get(announcedAddress);
        if (host != null && (peer = peers.get(host)) != null) {
            return peer;
        }
        try {
            URI uri = new URI("http://" + announcedAddress);
            host = uri.getHost();
            if (host == null) {
                return null;
            }
            // [NAT] if announcedAddress contains port then use ip+port as host
            int port = uri.getPort();
            if (port != -1) {
                host = host + ":" + port;
            }
            if ((peer = peers.get(host)) != null) {
                return peer;
            }
            String host2 = selfAnnouncedAddresses.get(host);
            if (host2 != null && (peer = peers.get(host2)) != null) {
                return peer;
            }
            InetAddress inetAddress = InetAddress.getByName(uri.getHost());
            return findOrCreatePeer(inetAddress, addressWithPort(announcedAddress), useNATService, create);
        } catch (URISyntaxException | UnknownHostException e) {
            Logger.logDebugMessage("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    static PeerImpl findOrCreatePeer(String host, boolean useNATService) {
        try {
            InetAddress inetAddress = InetAddress.getByName(useNATService?Peers.addressHost(host):host);
            return findOrCreatePeer(inetAddress, host, useNATService, true);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    static PeerImpl findOrCreatePeer(final InetAddress inetAddress, final String announcedAddress, final boolean useNATService, final boolean create) {
        PeerImpl peer;
        if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
            return null;
        }
        if (Peers.myAddress != null && Peers.myAddress.equalsIgnoreCase(announcedAddress)) {
            return null;
        }
        if (announcedAddress != null && announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
            return null;
        }
        String host = null;
        if (useNATService) {
            host = announcedAddress;
        } else {
            host = inetAddress.getHostAddress();
            if (Peers.cjdnsOnly && !host.substring(0, 2).equals("fc")) {
                return null;
            }
            //re-add the [] to ipv6 addresses lost in getHostAddress() above
            if (host.split(":").length > 2) {
                host = "[" + host + "]";
            }
        }
        if ((peer = peers.get(host)) != null) {
            return peer;
        }
        if (!create) {
            return null;
        }
        peer = new PeerImpl(host, announcedAddress);
        peer.setUseNATService(useNATService);
        if (!useNATService) {
            checkNetworkWhetherRight(host, peer.getPort() , false);
        }
        return peer;
    }

    static void checkNetworkWhetherRight(String host, int port, boolean abortWhenBadNetwork) {
        Logger.logInfoMessage("Network is " + Constants.getNetwork().getName() + ", host is " + host + ", port is " + port);
        host = StringUtils.isEmpty(host) ? "null" : host;
        String networkDetail = "";
        boolean badNetwork = false;
        if (Constants.isTestnet() && port != TESTNET_PEER_PORT) {
            networkDetail = "Peer host " + host + " on testnet is not using port " + TESTNET_PEER_PORT ;
            badNetwork = true;
        }else if (Constants.isDevnet() && port != DEVNET_PEER_PORT) {
            networkDetail = "Peer host " + host + " on devnet is not using port " + DEVNET_PEER_PORT ;
            badNetwork = true;
        }

        if (!Constants.isTestnet() && port == TESTNET_PEER_PORT) {
            networkDetail = "Peer host " + host + " is using testnet port " + port ;
            badNetwork = true;
        }else if (!Constants.isDevnet() && port == DEVNET_PEER_PORT) {
            networkDetail ="Peer host " + host + " is using devnet port " + port;
            badNetwork = true;
        }

        if(badNetwork && abortWhenBadNetwork) throw new RuntimeException(networkDetail);

        if(badNetwork) Logger.logDebugMessage(networkDetail,"ignoring");
    }


    static void setAnnouncedAddress(PeerImpl peer, String newAnnouncedAddress) {
        Peer oldPeer = peers.get(peer.getHost());
        if (oldPeer != null) {
            String oldAnnouncedAddress = oldPeer.getAnnouncedAddress();
            if (oldAnnouncedAddress != null && !oldAnnouncedAddress.equals(newAnnouncedAddress)) {
                Logger.logDebugMessage("Removing old announced address " + oldAnnouncedAddress + " for peer " + oldPeer.getHost());
                selfAnnouncedAddresses.remove(oldAnnouncedAddress);
            }
        }
        if (newAnnouncedAddress != null) {
            String oldHost = selfAnnouncedAddresses.put(newAnnouncedAddress, peer.getHost());
            if (oldHost != null && !peer.getHost().equals(oldHost)) {
                Logger.logDebugMessage("Announced address " + newAnnouncedAddress + " now maps to peer " + peer.getHost()
                        + ", removing old peer " + oldHost);
                oldPeer = peers.remove(oldHost);
                if (oldPeer != null) {
                    Peers.notifyListeners(oldPeer, Event.REMOVE);
                }
            }
        }
        peer.setAnnouncedAddress(newAnnouncedAddress);
    }

    public static boolean addPeer(Peer peer, String newAnnouncedAddress) {
        setAnnouncedAddress((PeerImpl)peer, newAnnouncedAddress.toLowerCase());
        return addPeer(peer);
    }

    public static boolean addPeer(Peer peer) {
        if (peers.put(peer.getHost(), (PeerImpl) peer) == null) {
            listeners.notify(peer, Event.NEW_PEER);
            return true;
        }
        return false;
    }

    public static PeerImpl removePeer(Peer peer) {
        if (peer.getAnnouncedAddress() != null) {
            selfAnnouncedAddresses.remove(peer.getAnnouncedAddress());
        }
        return peers.remove(peer.getHost());
    }

    public static void connectPeer(Peer peer) {
        peer.unBlacklist();
        ((PeerImpl)peer).connect();
    }

    public static void sendToSomePeers(Block block) {
        JSONObject request = block.getJSONObject();
        request.put("requestType", "processBlock");
        sendToSomePeers(request);
    }

    private static final int sendTransactionsBatchSize = 10;

    public static void sendToSomePeers(List<? extends Transaction> transactions) {
        int nextBatchStart = 0;
        while (nextBatchStart < transactions.size()) {
            JSONObject request = new JSONObject();
            JSONArray transactionsData = new JSONArray();
            for (int i = nextBatchStart; i < nextBatchStart + sendTransactionsBatchSize && i < transactions.size(); i++) {
                transactionsData.add(transactions.get(i).getJSONObject());
            }
            request.put("requestType", "processTransactions");
            request.put("transactions", transactionsData);
            sendToSomePeers(request);
            nextBatchStart += sendTransactionsBatchSize;
        }
    }

    private static void sendToSomePeers(final JSONObject request) {
        sendingService.submit(() -> {

            int successful = 0;
            List<Future<JSONObject>> expectedResponses = new ArrayList<>();
            for (final Peer peer : peers.values()) {

                if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
                    continue;
                }

                //TODO storage if non storage client send to

                if (!peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null
                        && peer.getBlockchainState() != Peer.BlockchainState.LIGHT_CLIENT) {
                    //[NAT] inject useNATService property to the request params
                    request.put("useNATService", Peers.isUseNATService());
                    request.put("announcedAddress", Peers.getMyAddress());
                    Future<JSONObject> futureResponse = peersService.submit(() -> peer.send(JSON.prepareRequest(request)));
                    expectedResponses.add(futureResponse);
                }
                if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
                    for (Future<JSONObject> future : expectedResponses) {
                        try {
                            JSONObject response = future.get();
                            if (response != null && response.get("error") == null) {
                                successful += 1;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            Logger.logDebugMessage("Error in sendToSomePeers", e);
                        }

                    }
                    expectedResponses.clear();
                }
                if (successful >= Peers.sendToPeersLimit) {
                    return;
                }
            }
        });
    }

    public static Peer getAnyPeer(final Peer.State state, final boolean applyPullThreshold) {
        return getWeightedPeer(getPublicPeers(state, applyPullThreshold));
    }

    public static List<Peer> getPublicPeers(final Peer.State state, final boolean applyPullThreshold) {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == state && peer.getAnnouncedAddress() != null
                && (!applyPullThreshold || !Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold));
    }

    public static Peer getWeightedPeer(List<Peer> selectedPeers) {
        if (selectedPeers.isEmpty()) {
            return null;
        }
        if (! Peers.enableHallmarkProtection || ThreadLocalRandom.current().nextInt(3) == 0) {
            return selectedPeers.get(ThreadLocalRandom.current().nextInt(selectedPeers.size()));
        }
        long totalWeight = 0;
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            totalWeight += weight;
        }
        long hit = ThreadLocalRandom.current().nextLong(totalWeight);
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            if ((hit -= weight) < 0) {
                return peer;
            }
        }
        return null;
    }

    static URI addressURI(String address) {
        try {
            URI uri = new URI("http://" + address);
            return uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    static String addressWithPort(String address) {
        if (address == null) {
            return null;
        }
        URI uri = addressURI(address);
        String host = uri.getHost();
        int port = uri.getPort();
        return port > 0 && port != Peers.getDefaultPeerPort() ? host + ":" + port : host;
    }

    static String addressHost(String address) {
        if (address == null) {
            return null;
        }
        URI uri = addressURI(address);
        return uri.getHost();
    }

    static int addressPort(String address) {
        if (address == null) {
            return -1;
        }
        URI uri = addressURI(address);
        return uri.getPort()>0?uri.getPort():Peers.getDefaultPeerPort();
    }

    public static boolean isUseNATService(String address) {
        if (address == null) {
            return false;
        }
        URI uri = addressURI(address);
        return uri.getPort()>0?(uri.getPort() != Peers.getDefaultPeerPort()?true:false):false;
    }

    public static boolean isOldVersion(String version, int[] minVersion) {
        if (version == null) {
            return true;
        }
        if (version.endsWith("e")) {
            version = version.substring(0, version.length() - 1);
        }
        String[] versions = version.split("\\.");
        for (int i = 0; i < minVersion.length && i < versions.length; i++) {
            try {
                int v = Integer.parseInt(versions[i]);
                if (v > minVersion[i]) {
                    return false;
                } else if (v < minVersion[i]) {
                    return true;
                }
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return versions.length < minVersion.length;
    }

    private static final int[] MAX_VERSION;
    static {
        String version = Conch.VERSION;
        if (version.endsWith("e")) {
            version = version.substring(0, version.length() - 1);
        }
        String[] versions = version.split("\\.");
        MAX_VERSION = new int[versions.length];
        for (int i = 0; i < versions.length; i++) {
            MAX_VERSION[i] = Integer.parseInt(versions[i]);
        }
    }

    public static boolean isNewVersion(String version) {
        if (version == null) {
            return true;
        }
        if (version.endsWith("e")) {
            version = version.substring(0, version.length() - 1);
        }
        String[] versions = version.split("\\.");
        for (int i = 0; i < MAX_VERSION.length && i < versions.length; i++) {
            try {
                int v = Integer.parseInt(versions[i]);
                if (v > MAX_VERSION[i]) {
                    return true;
                } else if (v < MAX_VERSION[i]) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return versions.length > MAX_VERSION.length;
    }

    public static boolean hasTooFewKnownPeers() {
        return peers.size() < Peers.minNumberOfKnownPeers;
    }

    public static boolean hasTooManyKnownPeers() {
        return peers.size() > Peers.maxNumberOfKnownPeers;
    }

    private static boolean hasEnoughConnectedPublicPeers(int limit) {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null
                && (! Peers.enableHallmarkProtection || peer.getWeight() > 0), limit).size() >= limit;
    }

    /**
     * Set the communication logging mask
     *
     * @param   events              Communication event list or null to reset communications logging
     * @return                      TRUE if the communication logging mask was updated
     */
    public static boolean setCommunicationLoggingMask(String[] events) {
        boolean updated = true;
        int mask = 0;
        if (events != null) {
            for (String event : events) {
                switch (event) {
                    case "EXCEPTION":
                        mask |= LOGGING_MASK_EXCEPTIONS;
                        break;
                    case "HTTP-ERROR":
                        mask |= LOGGING_MASK_NON200_RESPONSES;
                        break;
                    case "HTTP-OK":
                        mask |= LOGGING_MASK_200_RESPONSES;
                        break;
                    default:
                        updated = false;
                }
                if (!updated)
                    break;
            }
        }
        if (updated)
            communicationLoggingMask = mask;
        return updated;
    }

    /**
     * Return local peer services
     *
     * @return List of local peer services
     */
    public static List<Peer.Service> getServices() {
        return myServices;
    }

    private static void checkBlockchainState() {
        Peer.BlockchainState state = Constants.isLightClient ? Peer.BlockchainState.LIGHT_CLIENT :
                //区块链处于下载中 or 区块链中最后一个块的创建时间小于创世块(2013-10-24 12:00:00)的时间 -> 下载中
                (Conch.getBlockchainProcessor().isDownloading() || Conch.getBlockchain().getLastBlockTimestamp() < Conch.getEpochTime() - 600) ? Peer.BlockchainState.DOWNLOADING :
                        //区块链中最后一个块的基础难度除以基础难度大于10并且不是测试网络 -> 分叉 : 最新的
                        (Conch.getBlockchain().getLastBlock().getBaseTarget() / Constants.INITIAL_BASE_TARGET > 10 && !Constants.isTestnet()) ? Peer.BlockchainState.FORK :
                        Peer.BlockchainState.UP_TO_DATE;
        if (state != currentBlockchainState) {
            JSONObject json = new JSONObject(myPeerInfo);
            json.put("blockchainState", state.ordinal());
            json.put("peerLoad",getBestPeerLoad().toJson());
            myPeerInfoResponse = JSON.prepare(json);
            json.put("requestType", "getInfo");
            json.put("bestPeer",getBestPeerUri());
            myPeerInfoRequest = JSON.prepareRequest(json);
            currentBlockchainState = state;
        }
    }

    public static JSONStreamAware getMyPeerInfoRequest() {
        checkBlockchainState();
        return myPeerInfoRequest;
    }

    public static JSONStreamAware getMyPeerInfoResponse() {
        checkBlockchainState();
        return myPeerInfoResponse;
    }

    public static Peer.BlockchainState getMyBlockchainState() {
        checkBlockchainState();
        return currentBlockchainState;
    }

    public static PeerLoad getMyPeerLoad() {
        return myLoad;
    }

    public static String getBestPeer() {
        return bestPeer;
    }

    public static void setBestPeer(String peer) {
        bestPeer = peer;
    }
    private Peers() {} // never

    public static PeerLoad getBestPeerLoad(){
        return "127.0.0.1".equals(bestPeer) ? myLoad : peers.get(bestPeer).getPeerLoad();
    }

    public static String getBestPeerUri(){
        return "127.0.0.1".equals(bestPeer) ? "http://127.0.0.1:" + API.openAPIPort : peers.get(bestPeer).getPeerApiUri().toString();
    }
}
