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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockchainImpl;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.http.*;
import org.conch.http.biz.domain.ForkObj;
import org.conch.mint.Generator;
import org.conch.security.Guard;
import org.conch.tx.Transaction;
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
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.conch.common.Constants.bootNodesHost;
import static org.conch.peer.PeerDb.*;

/**
 * @author ben-xy
 */
public final class Peers {

    public enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER, ADD_INBOUND, REMOVE_INBOUND, CHANGED_SERVICES
    }
    public enum forkBlocksLevel {
        // 6
        MINI(6),
        // 18
        SMALL(18),
        // 144
        MEDIUM(144),
        // 432
        LONG(432);

        public final int level;

        forkBlocksLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static volatile int communicationLoggingMask;

    private static final List<String> wellKnownPeers;
    static final Set<String> knownBlacklistedPeers;

    static final int connectTimeout;
    static final int readTimeout;
    public static final int blacklistingPeriod;
    static final boolean getMorePeers;
    public static final boolean isProcessForkNode;
    static final boolean closeCollectFork;
    static final boolean isCommonNode;

    // change 20 -> 400, because one block include more than 2000 txs at 2018.11.21
    public static final int MAX_REQUEST_SIZE = 400 * 1024 * 1024;
    public static final int MAX_RESPONSE_SIZE = 400 * 1024 * 1024;
    public static final int MAX_MESSAGE_SIZE = 400 * 1024 * 1024;
    public static final int MIN_COMPRESS_SIZE = 256;
    static final boolean useWebSockets;
    static final int webSocketIdleTimeout;

    public static String getMyAddress() {
        return Conch.getMyAddress();
    }

    public static boolean isUseNATService() {
        return Conch.isUseNAT();
    }
    
    static Map<String,Object> natAndAddrMap = null;
    public static Map<String,Object> getNatAndAddressMap(){
        if(natAndAddrMap == null) {
            natAndAddrMap = Maps.newHashMap();
            natAndAddrMap.put("useNATService", Peers.isUseNATService());
            natAndAddrMap.put("announcedAddress", Peers.getMyAddress());
        }
        return natAndAddrMap;
    }

    public static boolean isMyAddressAnnounced() {
        return Conch.getMyAddress() != null;
    }

    static final boolean useProxy = System.getProperty("socksProxyHost") != null || System.getProperty("http.proxyHost") != null;
    static final boolean isGzipEnabled;

    private static final int MAX_PUBLIC_PEER_CONNECT_IN_TEST_OR_DEV = 50;
    private static final String myPlatform;
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

    /** my peer info, it be generated in the static block **/
    private static final JSONObject myPeerInfo;
    private static List<Peer.Service> myServices = Lists.newArrayList();
    private static volatile Peer.BlockchainState currentBlockchainState;
    private static volatile JSONStreamAware myPeerInfoRequest;
    private static volatile JSONStreamAware myPeerInfoResponse;

    private static final Listeners<Peer, Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> selfAnnouncedAddresses = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    static final ExecutorService peersService = new QueuedThreadPool(2, 15);
    private static final ExecutorService sendingService = Executors.newFixedThreadPool(10);

    private static final boolean enableBizAPIs = Conch.getBooleanProperty("sharder.enableBizAPIs");
    private static final boolean enableStorage = Conch.getBooleanProperty("sharder.storage.enable");

    private static List<String> loadPeersSetting() {
        return Constants.isTestnetOrDevnet() ? Conch.getStringListProperty("sharder.defaultTestnetPeers") : Conch.getStringListProperty("sharder.defaultPeers");
    }

//    /**
//     * this method must be called after #checkOpenServices()
//     * @param service 
//     * @return
//     */
//    public static boolean openService(Peer.Service service){ 
//        for(Peer.Service myService : myServices){
//            if(myService.ordinal() == service.ordinal()) {
//                return true;
//            }
//        }
//       return false;
//    }
    
    private static List<Peer.Service> checkOpenServices() {
        List<Peer.Service> servicesList = Lists.newArrayList();
        /** basic infos **/
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            servicesList.add(Peer.Service.HALLMARK);
        }

        /** apis of my peer **/
        if (!Constants.ENABLE_PRUNING && Constants.INCLUDE_EXPIRED_PRUNABLE) {
            servicesList.add(Peer.Service.PRUNABLE);
        }
        if (API.openAPIPort > 0) {
            servicesList.add(Peer.Service.API);
        }
        if (API.openAPISSLPort > 0) {
            servicesList.add(Peer.Service.API_SSL);
        }

        if ((API.openAPIPort > 0 || API.openAPISSLPort > 0) && API.apiServerCORS) {
            servicesList.add(Peer.Service.CORS);
        }

        /** services of my peer **/
        // Add Business API service
        if (enableBizAPIs) {
            servicesList.add(Peer.Service.BAPI);
        }
        // Add Storage service
        if (enableStorage) {
            servicesList.add(Peer.Service.STORAGE);
        }
        return servicesList;
    }
    
    /**
     * generate my peer info in static block to ake sure the final params is right
     */
    static {
        String platform = Conch.getStringProperty("sharder.myPlatform", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        if (platform.length() > MAX_PLATFORM_LENGTH) {
            platform = platform.substring(0, MAX_PLATFORM_LENGTH);
        }
        myPlatform = platform;

        String myHost = null;
        int myPort = -1;
        if (Conch.getMyAddress() != null) {
            try {
                URI uri = new URI("http://" + Conch.getMyAddress());
                myHost = uri.getHost();
                myPort = (uri.getPort() == -1 ? Conch.getPeerPort() : uri.getPort());
                boolean addrValid = false;
                InetAddress[] myAddrs = InetAddress.getAllByName(myHost);
                Enumeration<NetworkInterface> intfs = NetworkInterface.getNetworkInterfaces();
                chkAddr: while (intfs.hasMoreElements()) {
                    NetworkInterface intf = intfs.nextElement();
                    List<InterfaceAddress> intfAddrs = intf.getInterfaceAddresses();
                    for (InterfaceAddress intfAddr : intfAddrs) {
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
        checkNetworkWhetherRight(myHost, Conch.getPeerPort());
        shareMyAddress = Conch.getBooleanProperty("sharder.shareMyAddress") && !Constants.isOffline;
        enablePeerUPnP = Conch.getBooleanProperty("sharder.enablePeerUPnP");
        myHallmark = Convert.emptyToNull(Conch.getStringProperty("sharder.myHallmark", "").trim());
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (!hallmark.isValid()) {
                    throw new RuntimeException("Hallmark is not valid");
                }
                if (Conch.getMyAddress() != null) {
                    if (!hallmark.getHost().equals(myHost)) {
                        throw new RuntimeException("Invalid hallmark host");
                    }
                    if (myPort != hallmark.getPort()) {
                        throw new RuntimeException("Invalid hallmark port");
                    }
                }
            } catch (RuntimeException e) {
                Logger.logErrorMessage("Your hallmark is invalid: " + Peers.myHallmark + " for your address: " + Conch.getMyAddress() + "[" + e.getMessage() + "]");
                throw new RuntimeException(e.toString(), e);
            }
        }
        List<Peer.Service> servicesList = new ArrayList<>();
        JSONObject myPeerInfoJson = new JSONObject();

        /** announced address and host of my peer **/
        if (Conch.getMyAddress() != null) {
            try {
                URI uri = new URI("http://" + Conch.getMyAddress());
                String host = uri.getHost();
                int port = uri.getPort();
                String announcedAddress;
                if (!Constants.isTestnetOrDevnet()) {
                    if (port >= 0) {
                        announcedAddress = Conch.getMyAddress();
                    } else {
                        announcedAddress = host + ((configuredServerPort != Conch.PresetParam.DEFAULT_PEER_PORT) ? (":" + configuredServerPort) : "");
                    }
                } else {
                    //[NAT] Lanproxy use serverIP+port(for client),so use host+port = myAddress
                    announcedAddress = Conch.getMyAddress();
                }
                if (announcedAddress == null || announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
                    throw new RuntimeException("Invalid announced address length: " + announcedAddress);
                }
                myPeerInfoJson.put("announcedAddress", announcedAddress);
            } catch (URISyntaxException e) {
                Logger.logMessage("Your announce address is invalid: " + Conch.getMyAddress());
                throw new RuntimeException(e.toString(), e);
            }
        }

        /** basic infos **/
        myPeerInfoJson.put("useNATService", Conch.isUseNAT());
        myPeerInfoJson.put("application", Conch.APPLICATION);
        myPeerInfoJson.put("version", Conch.VERSION);
        myPeerInfoJson.put("platform", Peers.myPlatform);
        myPeerInfoJson.put("shareAddress", Peers.shareMyAddress);
        myPeerInfoJson.put("cosUpdateTime", Conch.getCosUpgradeDate());
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
            myPeerInfoJson.put("disabledAPIs", APIEnum.enumSetToBase64String(disabledAPISet));
            myPeerInfoJson.put("apiServerIdleTimeout", API.apiServerIdleTimeout);
        }

        /** apis of my peer **/
        myServices = checkOpenServices();
        
        if(myServices.contains(Peer.Service.HALLMARK)) {
            myPeerInfoJson.put("hallmark", Peers.myHallmark);
        }
        
        if(myServices.contains(Peer.Service.API)) {
            myPeerInfoJson.put("apiPort", API.openAPIPort);
        }
        
        if(myServices.contains(Peer.Service.API_SSL)) {
            myPeerInfoJson.put("apiSSLPort", API.openAPISSLPort);
        }
        
        if(myServices.contains(Peer.Service.BAPI)) {
            myPeerInfoJson.put("enableBizAPIs", true);
        }
        
        if(myServices.contains(Peer.Service.STORAGE)) {
            myPeerInfoJson.put("enableStorage", true);
        }
  
        myPeerInfoJson.put("services", Long.toUnsignedString(getServicesInLong()));

        /** mint accounts of my peer **/
        String autoMintRs = Generator.getAutoMiningRS();
        if (StringUtils.isNotEmpty(autoMintRs)) {
            myPeerInfoJson.put("bindRsAccount", autoMintRs);
        }

        /** running mode of my peer **/
        myPeerInfoJson.put("runningMode", Conch.runningMode.getName());

        Logger.logDebugMessage("My peer info:\n" + myPeerInfoJson.toJSONString());
        myPeerInfo = myPeerInfoJson;
        myLoad = new PeerLoad("127.0.0.1", API.openAPIPort, -1);

        final List<String> defaultPeers = loadPeersSetting();
        wellKnownPeers = parseWellknownPeers();

        List<String> knownBlacklistedPeersList = Conch.getStringListProperty("sharder.knownBlacklistedPeers");
        if (knownBlacklistedPeersList.isEmpty()) {
            knownBlacklistedPeers = Collections.emptySet();
        } else {
            knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
        }

        maxNumberOfInboundConnections = Conch.getIntProperty("sharder.maxNumberOfInboundConnections");
        maxNumberOfOutboundConnections = Conch.getIntProperty("sharder.maxNumberOfOutboundConnections");
        maxNumberOfConnectedPublicPeers = (Constants.isTestnetOrDevnet()) ? Math.max(Conch.getIntProperty("sharder.maxNumberOfConnectedPublicPeers"), MAX_PUBLIC_PEER_CONNECT_IN_TEST_OR_DEV) : Math.min(Conch.getIntProperty("sharder.maxNumberOfConnectedPublicPeers"),
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
        usePeersDb = Conch.getBooleanProperty("sharder.usePeersDb") && !Constants.isOffline;
        savePeers = usePeersDb && Conch.getBooleanProperty("sharder.savePeers");
        getMorePeers = Conch.getBooleanProperty("sharder.getMorePeers");
        cjdnsOnly = Conch.getBooleanProperty("sharder.cjdnsOnly");
        ignorePeerAnnouncedAddress = Conch.getBooleanProperty("sharder.ignorePeerAnnouncedAddress");
        if (useWebSockets && useProxy) {
            Logger.logMessage("Using a proxy, will not create outbound websockets.");
        }
        closeCollectFork = Conch.getBooleanProperty("sharder.closeCollectFork");
        isProcessForkNode = Conch.getBooleanProperty("sharder.isProcessForkNode");
        isCommonNode = !isProcessForkNode && !isCollectForkNode(getMyAddress());

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
    
    private static List<String> parseWellknownPeers(){
        List<String> peers = Constants.isMainnet() ? Conch.getStringListProperty("sharder.wellKnownPeers") : Conch.getStringListProperty("sharder.testnetPeers");
        
        List<String> hosts = Lists.newArrayList();
        for(String peerStr : peers){
            String host = peerStr;
            String type;
            if(StringUtils.isNotEmpty(peerStr) && peerStr.contains("#")){
                String[] peerArray = peerStr.split("#");
                host = peerArray[0];
                type = peerArray[1];
            }
            
            if(StringUtils.isNotEmpty(host)) {
                hosts.add(host);
                //TODO[valid-node] consider add these wellknown peers into certified node list
            }
        }
        return Collections.unmodifiableList(hosts);
    }


    private static class Init {

        private final static Server peerServer;

        static {
            if (Peers.shareMyAddress) {
                peerServer = new Server();
                ServerConnector connector = new ServerConnector(peerServer);
                final int port = Conch.getPeerPort();
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
                    FilterHolder dosFilterHolder = ctxHandler.addFilter(DoSFilter.class, "/*",EnumSet.of(DispatcherType.REQUEST));
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
                                if (peerConnector instanceof ServerConnector) {
                                    UPnP.addPort(((ServerConnector) peerConnector).getPort());
                                }
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
        }

        private static void init() {
        }

        private Init() {
        }

    }

    private static final Runnable peerUnBlacklistingThread = () -> {

        try {
            try {

                int curTime = Conch.getEpochTime();
                for (PeerImpl peer : peers.values()) {
                    peer.updateBlacklistedStatus(curTime);
                    Guard.checkAndRemoveSelfClosingPeer(peer.getHost(), curTime);
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
                                connectSet.add((PeerImpl) peerList.get(ThreadLocalRandom.current().nextInt(peerList.size())));
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
                                && isCollectForkNode(peer.getAddress())
                                && now - peer.getLastUpdated() > 600) {
                            peersService.submit(peer::connect);
                        }
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
                if (!"127.0.0.1".equals(bestPeer) && myServices.contains(Peer.Service.BAPI)
                        && getMyPeerLoad().getLoad() < peers.get(bestPeer).getPeerLoad().getLoad()) {
                    bestPeer = "127.0.0.1";
                }
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
                System.exit(1);
            }

        }

    };

    private static Map<String, ForkObj> forkObjMap = Maps.newHashMap();
    private static Map<Long, ForkBlock.ForkBlockObj> forkBlockObjMap = Maps.newHashMap();
    private static Map<String, List<JSONObject>> forkBlocksMap = Maps.newHashMap();
    private static Map<String, List<JSONObject>> forkBlocksMapByProcessNode = Maps.newHashMap();
    public static Map<String, JSONObject> missingForkBlocksMap = Maps.newHashMap();

    public static Map<String, ForkObj> getForkObjMap() {
        return forkObjMap;
    }

    public static Map<String, ForkObj> getForkObjMapToAPI() {
        return forkObjMapToAPI;
    }
    public static Map<Long, ForkBlock.ForkBlockObj> getForkBlockObjMapToAPI() {
        return forkBlockObjMapToAPI;
    }

    private static Map<String, ForkObj> forkObjMapToAPI = Maps.newHashMap();
    private static Map<Long, ForkBlock.ForkBlockObj> forkBlockObjMapToAPI = Maps.newHashMap();

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
                    request.put("announcedAddress", Conch.getMyAddress());
                    JSONObject response = peer.send(JSON.prepareRequest(request), Peers.MAX_RESPONSE_SIZE);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray) response.get("peers");
                    Set<String> addedAddresses = new HashSet<>();
                    if (peers != null) {
                        JSONArray services = (JSONArray) response.get("services");
                        boolean setServices = (services != null && services.size() == peers.size());
                        int now = Conch.getEpochTime();
                        for (int i = 0; i < peers.size(); i++) {
                            String announcedAddress = (String) peers.get(i);
                            PeerImpl newPeer = findOrCreatePeer(announcedAddress, Peers.isUseNATService(announcedAddress));
                            if (newPeer != null) {
                                if (now - newPeer.getLastUpdated() > 24 * 3600) {
                                    newPeer.setLastUpdated(now);
                                    updatedPeer = true;
                                }
                                if (Peers.addPeer(newPeer) && setServices) {
                                    newPeer.setServices(Long.parseUnsignedLong((String) services.get(i)));
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
                        request.put("announcedAddress", Conch.getMyAddress());
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
            try {
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
                    if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted() && now - peer.getLastUpdated() < 7 * 24 * 3600) {
                        currentPeers.put(peer.getAnnouncedAddress(),
                                new PeerDb.Entry(peer.getAnnouncedAddress(), peer.getServices(), peer.getLastUpdated()));
                    }
                });
                //
                // Build toDelete and toUpdate lists
                //
                List<PeerDb.Entry> toDelete = new ArrayList<>(oldPeers.size());
                oldPeers.forEach(entry -> {
                    if (currentPeers.get(entry.getAddress()) == null) {
                        toDelete.add(entry);
                    }
                });
                List<PeerDb.Entry> toUpdate = new ArrayList<>(currentPeers.size());
                currentPeers.values().forEach(entry -> {
                    PeerDb.Entry oldEntry = oldMap.get(entry.getAddress());
                    if (oldEntry == null || entry.getLastUpdated() - oldEntry.getLastUpdated() > 24 * 3600) {
                        toUpdate.add(entry);
                    }
                });
                //
                // Nothing to do if all of the lists are empty
                //
                if (toDelete.isEmpty() && toUpdate.isEmpty()) {
                    return;
                }
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
            } catch (Exception e) {
                e.printStackTrace();
                Logger.logErrorMessage("update saved Peers fail " + e);
            }
        }

    };

    public static int deleteHeight() {
        if (maxHeight != 0) {
            return maxHeight - forkBlocksLevel.LONG.getLevel();
        } else {
            return Conch.getHeight() - forkBlocksLevel.LONG.getLevel();
        }
    }

    /**
     * Get database related data, every 24h execute once
     */
    private static final Runnable updateForkBlockDataThread = new Runnable() {
        @Override
        public void run() {
            DbIterator<ForkBlock> forkBlocks = null;
            try {
                deleteForkBlocksAndLinkedFromHeight(deleteHeight());
                blockLinkedGeneratorMap.clear();
                blockLinkedGeneratorHeightMap.clear();
                blockLinkedGeneratorMap = getAllForkBlockLinkedAccountMap();
                for (Map.Entry<String, HashSet<ForkBlock.ForkBlockLinkedAccount>> entry : blockLinkedGeneratorMap.entrySet()) {
                    TreeSet<Integer> treeSet = Sets.newTreeSet();
                    for (ForkBlock.ForkBlockLinkedAccount linkedAccount : entry.getValue()) {
                        treeSet.add(linkedAccount.getHeight());
                        blockLinkedGeneratorHeightMap.put(entry.getKey(), treeSet);
                    }
                }
                blockIdsSet.clear();
                blockMap.clear();
                forkBlocks = getAllForkBlocks();
                for (ForkBlock forkBlock : forkBlocks) {
                    blockMap.put(forkBlock.getId(), forkBlock);
                    blockIdsSet.add(forkBlock.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                DbUtils.close(forkBlocks);
            }
        }
    };

    private static final String SC_PEERS_API = UrlManager.getFoundationUrl(
            UrlManager.PEERS_LIST_EOLINKER,
            UrlManager.PEERS_LIST_LOCAL,
            UrlManager.PEERS_LIST_PATH
    );

    /** Syn cetified peers from foundation
     *
    public static boolean synCertifiedPeers() throws Exception {
        String peersStr = Https.httpRequest(SC_PEERS_API, "GET", null);
        com.alibaba.fastjson.JSONArray peerArrayJson = new com.alibaba.fastjson.JSONArray();
        if (StringUtils.isEmpty(peersStr)) {
            Logger.logInfoMessage("ge peer list from %s is null, no needs to get peer info", SC_PEERS_API);
            return false;
        } else {
            if (peersStr.startsWith(Constants.BRACKET)) {
                peerArrayJson = com.alibaba.fastjson.JSON.parseArray(peersStr);
            } else if (peersStr.startsWith(Constants.CURLY_BRACES)) {
                peerArrayJson.add(com.alibaba.fastjson.JSON.parseObject(peersStr));
            }
        }

        String detail = "\n\r==================> syn certified peers from [" + SC_PEERS_API + "] and found size " + peerArrayJson.size() + " peers\n\r";
        Iterator iterator = peerArrayJson.iterator();
        while (iterator.hasNext()) {
            com.alibaba.fastjson.JSONObject peerJson = (com.alibaba.fastjson.JSONObject) iterator.next();

            String host = peerJson.getString("announcedAddress");
            if (StringUtils.isEmpty(host)) {
                host = peerJson.getString("address");
            }

            String bindAddress = peerJson.getString("bindRs");
            Peer peer = Peers.getPeer(host, true);
            if (StringUtils.isEmpty(bindAddress)) {
                detail += "can't process peer[host=" + host + "] which rs address is null\n\r";
                continue;
            }

            if (peer == null) {
                peer = findOrCreatePeer(host, Peers.isUseNATService(host), true);
                if (peer != null) {
                    Peers.addPeer(peer, host);
                    Peers.connectPeer(peer);
                }
                peer = Peers.getPeer(host, true);
                detail += "create a new certified peer[host=" + host + ",linked rs=" + bindAddress + "]\n\r";
            } else {
                detail += "update a certified peer[host=" + host + ",linked rs=" + bindAddress + "]\n\r";
            }

//            if(peer != null) {
//                peer.setBindRsAccount(bindAddress);
//                Conch.getPocProcessor().updateBoundPeer(host, Account.rsAccountToId(bindAddress));
//            }
        }
        detail += "<================== certified peer info updated";
        Logger.logDebugMessage(detail);
        return true;
    }


     // get and update the local bound rs account of certified peer
    private static final Runnable GET_CERTIFIED_PEER_THREAD = () -> {
        try {
            synCertifiedPeers();
        } catch (Exception e) {
            Logger.logErrorMessage("syn certified peer thread interrupted, wait for next round", e);
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };
     */

    public static volatile boolean hardwareTested = false;
    public static volatile boolean sysInitialed = false;
    public static volatile boolean hasMyAddress = StringUtils.isNotEmpty(Conch.getMyAddress());
    public static final int DEFAULT_TX_CHECKING_COUNT = 40;
    private static final Runnable HARDWARE_TESTING_THREAD = () -> {
        if (!sysInitialed && hasMyAddress) {
            Logger.logInfoMessage("Wait Conch initialized to test the hardware performance, sleep 60S...");
            return;
        }

        if (!hasMyAddress) {
            Logger.logInfoMessage("Current node configuration not initialized yet, sleep 60S...");
            return;
        }

        if (hardwareTested) {
            return;
        }

        hardwareTested = GetNodeHardware.readAndReport(DEFAULT_TX_CHECKING_COUNT);
    };


    static {
        // listener and thread run
        Peers.addListener(peer -> peersService.submit(() -> {
            if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted()) {
                try {
                    Db.db.beginTransaction();
                    PeerDb.updatePeer((PeerImpl) peer);
                    Db.db.commitTransaction();
                } catch (RuntimeException e) {
                    Logger.logErrorMessage("Unable to update peer database", e);
                    Db.db.rollbackTransaction();
                } finally {
                    Db.db.endTransaction();
                }
            }
        }), Peers.Event.CHANGED_SERVICES);

        Account.addListener(account -> peers.values().forEach(peer -> {
            if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                Peers.listeners.notify(peer, Event.WEIGHT);
            }
        }), Account.Event.BALANCE);

        if (!Constants.isOffline) {
            ThreadPool.scheduleThread("PeerConnecting", Peers.peerConnectingThread, 20);
            ThreadPool.scheduleThread("PeerUnBlacklisting", Peers.peerUnBlacklistingThread, 60);
            if (Peers.getMorePeers) {
                ThreadPool.scheduleThread("GetMorePeers", Peers.getMorePeersThread, 20);
            }
            if (isProcessForkNode) {
                ThreadPool.scheduleThread("UpdateForkBlockData", Peers.updateForkBlockDataThread, 1, TimeUnit.HOURS);
            }
        }
    }

    public static void init() {
        Init.init();
//        ThreadPool.scheduleThread("GetCertifiedPeer", Peers.GET_CERTIFIED_PEER_THREAD, 1, TimeUnit.MINUTES);
        ThreadPool.scheduleThread("PeerHardwareTesting", Peers.HARDWARE_TESTING_THREAD, 7, TimeUnit.DAYS);
    }

    public static void shutdown() {
        if (Init.peerServer != null) {
            try {
                Init.peerServer.stop();
                if (enablePeerUPnP) {
                    Connector[] peerConnectors = Init.peerServer.getConnectors();
                    for (Connector peerConnector : peerConnectors) {
                        if (peerConnector instanceof ServerConnector) {
                            UPnP.deletePort(((ServerConnector) peerConnector).getPort());
                        }
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

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static List<Peer> getActivePeers() {
        return getPeers(peer -> peer.getState() != Peer.State.NON_CONNECTED);
    }

    public static List<Peer> getPeers(final Peer.State state) {
        return getPeers(peer -> peer.getState() == state);
    }

    public static List<Peer> getPeers(List<Peer.Type> types) {
        List<Peer> result = new ArrayList<>();
        for (Peer peer : peers.values()) {
            for (Peer.Type type : types) {
                if (peer.isType(type)) {
                    result.add(peer);
                }
            }
        }
        return result;
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

    /**
     * find the peer by specified host
     * @param host peer's host
     * @param checkAnnouncedAddr true-compare the host with the announced addr of peer
     * @return
     */
    public static Peer getPeer(String host, boolean checkAnnouncedAddr) {
        if(!checkAnnouncedAddr) {
            return peers.get(host);
        }
        
        if(peers.containsKey(host)) {
            return peers.get(host);
        }
        
        // check by announced address
        for(PeerImpl peer : peers.values()){
            if(host.equals(peer.getAnnouncedAddress())){
                return peer;
            }
        }
        return null;
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
            InetAddress inetAddress = InetAddress.getByName(useNATService ? Peers.addressHost(host) : host);
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
        if (Conch.getMyAddress() != null && Conch.getMyAddress().equalsIgnoreCase(announcedAddress)) {
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
            checkNetworkWhetherRight(host, peer.getPort());
        }
        return peer;
    }

    static void checkNetworkWhetherRight(String host, int port) {
//        Logger.logDebugMessage("Check the format of the peer's address [ host is " + host + ", port is " + port + ", network is" + Constants.getNetwork().getName() + " ]");
        host = StringUtils.isEmpty(host) ? "null" : host;
        String networkDetail = "";
        boolean badNetwork = false;
        if (Constants.isTestnet() && port != Conch.PresetParam.getPeerPort(Constants.Network.TESTNET)) {
            networkDetail = "Peer host " + host + " on testnet is not using port " + Conch.PresetParam.getPeerPort(Constants.Network.TESTNET);
            badNetwork = true;
        } else if (Constants.isDevnet() && port != Conch.PresetParam.getPeerPort(Constants.Network.DEVNET)) {
            networkDetail = "Peer host " + host + " on devnet is not using port " + Conch.PresetParam.getPeerPort(Constants.Network.DEVNET);
            badNetwork = true;
        }

        if (!Constants.isTestnet() && port == Conch.PresetParam.getPeerPort(Constants.Network.TESTNET)) {
            networkDetail = "Peer host " + host + " is using testnet port " + port;
            badNetwork = true;
        } else if (!Constants.isDevnet() && port == Conch.PresetParam.getPeerPort(Constants.Network.DEVNET)) {
            networkDetail = "Peer host " + host + " is using devnet port " + port;
            badNetwork = true;
        }

        if (badNetwork) {
            Logger.logDebugMessage(networkDetail, "ignoring");
        }
    }
    
    private static boolean hostSameAsAddress(Peer peer){
        if(peer == null) {
            return false;
        }
        if(StringUtils.isEmpty(peer.getAnnouncedAddress())) {
            return false;
        }
        if(StringUtils.isEmpty(peer.getHost())) {
            return false;
        }
        
        return StringUtils.equalsIgnoreCase(peer.getAnnouncedAddress(), peer.getHost());
    }
    
    static void setAnnouncedAddress(PeerImpl peer, String newAnnouncedAddress) {
        if(StringUtils.isEmpty(newAnnouncedAddress)) {
            return;
        }

        Peer oldPeer = peers.get(peer.getHost());
        if (oldPeer != null) {
            String oldAnnouncedAddress = oldPeer.getAnnouncedAddress();
            if (oldAnnouncedAddress != null && !oldAnnouncedAddress.equals(newAnnouncedAddress)) {
                Logger.logDebugMessage("Removing old announced address " + oldAnnouncedAddress + " for peer " + oldPeer.getHost());
                selfAnnouncedAddresses.remove(oldAnnouncedAddress);
            }
        }
        
        if (StringUtils.isNotEmpty(newAnnouncedAddress)) {
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
        setAnnouncedAddress((PeerImpl) peer, newAnnouncedAddress.toLowerCase());
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
        ((PeerImpl) peer).connect();
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

                //TODO[storage] storage if non storage client send to

                if (!peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null
                        && peer.getBlockchainState() != Peer.BlockchainState.LIGHT_CLIENT) {
                    //[NAT] inject useNATService property to the request params
                    request.put("useNATService", Peers.isUseNATService());
                    request.put("announcedAddress", Conch.getMyAddress());
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


    private static Map<String,Integer> weightedPeerCountMap = Maps.newConcurrentMap();
    private static int MAX_CONNECT_CHECK_COUNT = (bootNodesHost.size() + 3) * 3;
    /**
     *  true: count < 3
     *  false: 3 =< count <= 6
     *  reset the count to 0 and re-calculate: count > 6
     * @param peer
     * @return
     */
    private synchronized static boolean countAndCheck(Peer peer) {
        if(!weightedPeerCountMap.containsKey(peer.getHost())) {
            weightedPeerCountMap.put(peer.getHost(), 0);
        }
        int connectCount = weightedPeerCountMap.get(peer.getHost());
    
        if(connectCount >= 3 && connectCount < MAX_CONNECT_CHECK_COUNT) {
            weightedPeerCountMap.put(peer.getHost(), connectCount+1);
            return false;
        } else if(connectCount >= MAX_CONNECT_CHECK_COUNT){
            weightedPeerCountMap.put(peer.getHost(), 0);
            return false;
        } else {
            weightedPeerCountMap.put(peer.getHost(), connectCount+1);
        }

        // cos version compare 
        return  Conch.versionCompare(peer.getVersion()) <= 0;
    }
    /**
     * remote node's cos version should larger than current cos version, and priority sequences is:
     * - connected boot nodes
     * - connected well-known nodes
     * - normal nodes sorted by weight
     * @param selectedPeers
     * @return
     */
    public static Peer getWeightedPeer(List<Peer> selectedPeers) {
        if (selectedPeers.isEmpty()) {
            return null;
        }

        long totalWeight = 0;
        for (Peer peer : selectedPeers) {
            long weight = (peer.getWeight() == 0) ? 1 : peer.getWeight();
            totalWeight += weight;
            // return boot node directly
//            if(Constants.isValidBootNode(peer)
//             && countAndCheck(peer)) {
//                return peer;
//            }
            if(countAndCheck(peer)) {
                return peer;
            }
        }

        if (!Peers.enableHallmarkProtection || ThreadLocalRandom.current().nextInt(3) == 0) {
            Peer randomPeer = selectedPeers.get(ThreadLocalRandom.current().nextInt(selectedPeers.size()));
            if(countAndCheck(randomPeer)) {
                return randomPeer;
            }
        }
        
        long hit = ThreadLocalRandom.current().nextLong(totalWeight);
        for (Peer peer : selectedPeers) {
            long weight = (peer.getWeight() == 0) ? 1 : peer.getWeight();
            boolean rightVersion = Conch.versionCompare(peer.getVersion()) <= 0;
            if ((hit -= weight) < 0
            && rightVersion) {
//                if(countAndCheck(peer)) {
//                    return peer;
//                }

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
        return port > 0 && port != Conch.getPeerPort() ? host + ":" + port : host;
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
        return uri.getPort() > 0 ? uri.getPort() : Conch.getPeerPort();
    }

    public static boolean isUseNATService(String address) {
        if (address == null) {
            return false;
        }
        URI uri = addressURI(address);
        return uri.getPort() > 0 ? (uri.getPort() != Conch.getPeerPort() ? true : false) : false;
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
                && (!Peers.enableHallmarkProtection || peer.getWeight() > 0), limit).size() >= limit;
    }

    /**
     * Set the communication logging mask
     *
     * @param events Communication event list or null to reset communications logging
     * @return TRUE if the communication logging mask was updated
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
                if (!updated) {
                    break;
                }
            }
        }
        if (updated) {
            communicationLoggingMask = mask;
        }
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

    public static long getServicesInLong() {
        return _mapServiceToLong(myServices);
    }

    private static long _mapServiceToLong(List<Peer.Service> servicesList) {
        long services = 0;
        for (Peer.Service service : servicesList) {
            services |= service.getCode();
        }
        return services;
    }
    
    public static JSONObject getBlockchainSummary(){
        JSONObject json = new JSONObject();
        Block lastBlock = Conch.getBlockchain().getLastBlock();
        if(lastBlock != null){
            json.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
            json.put("lastBlockHeight", lastBlock.getHeight());
            json.put("lastBlockId", lastBlock.getId());
            json.put("lastBlockHash",  Convert.toHexString(lastBlock.getPayloadHash()));
            json.put("lastBlockGenerator", Account.rsAccount(lastBlock.getGeneratorId()));
            json.put("lastBlockTimestamp", Convert.dateFromEpochTime(lastBlock.getTimestamp()));
            json.put("currentFork", ForceConverge.currentFork);
        }
        return json;
    }

    public static boolean isCollectForkNode(String announcedAddress) {
        ArrayList<Object> collectForkNodes = Lists.newArrayList();
        collectForkNodes.addAll(bootNodesHost);
        collectForkNodes.add("testnc.mw.run");
        return collectForkNodes.contains(announcedAddress) && !Peers.closeCollectFork;
    }

    public static JSONArray getForkBlocks(Integer startNum, Integer endNum) {
        JSONArray blocks = new JSONArray();
        DbIterator<? extends Block> iterator = null;
        final int timestamp = 0;
        try {
            iterator = Conch.getBlockchain().getBlocksByHeight(startNum + 1, endNum, null);
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getTimestamp() < timestamp) {
                    break;
                }
                blocks.add(JSONData.forkBlock(block));
            }
        }finally {
            DbUtils.close(iterator);
        }
        return blocks;
    }

    public static JSONObject additionalBlockHeightObj = new JSONObject();

    /**
     * - commonNode: report forkBlocks to the designated node (BootNode)
     * - collectForkNode: return forkObjMap to processForkNode
     * - processForkNode: return identification
     *
     * Current call frequency = 600s < f < 620s, have check
     * @param conditionObj
     * @return json
     */
    public static JSONObject getForkBlockSummary(JSONObject conditionObj){
        JSONObject json = new JSONObject();
        if (conditionObj == null) {
            return json;
        }
        if (conditionObj.get("sendToCollectForkNode") != null) {
            if (isProcessForkNode) {
                json.put("processForkNode", true);
                json.put("missedBlocksMap", missingForkBlocksMap);
            } else {
                if (!additionalBlockHeightObj.isEmpty()) {
                    Integer[] maxAndMinArray = getMaxAndMinArray();
                    JSONArray forkBlocks = getForkBlocks(maxAndMinArray[0], maxAndMinArray[1]);
                    json.put("forkBlocks", forkBlocks);
                } else {
                    JSONArray forkBlocks = getForkBlocks(Conch.getHeight() - forkBlocksLevel.MINI.getLevel(), Conch.getHeight());
                    json.put("forkBlocks", forkBlocks);
                }
            }
        }
        if (conditionObj.get("sendToProcessForkNode") != null){
            if (Generator.HUB_BIND_ADDRESS != null) {
                if (missingForkBlocksMap.get(Generator.HUB_BIND_ADDRESS) != null) {
                    additionalBlockHeightObj = missingForkBlocksMap.get(Generator.HUB_BIND_ADDRESS);
                    Integer[] maxAndMinArray = getMaxAndMinArray();
                    forkBlocksMap.put(Generator.HUB_BIND_ADDRESS, Peers.getForkBlocks(maxAndMinArray[0], maxAndMinArray[1]));
                } else {
                    forkBlocksMap.put(Generator.HUB_BIND_ADDRESS, getForkBlocks(Conch.getHeight()-forkBlocksLevel.MINI.getLevel(), Conch.getHeight()));
                }

            }
            if (!Peers.forkBlocksMap.isEmpty()) {
                json.put("forkBlocksMap", Peers.forkBlocksMap);
            }
        }
        if (conditionObj.get("sendToCommonNode") != null) {
            json.put("missedBlocks", missingForkBlocksMap.get(conditionObj.get("sendToCommonNode")));
        }
        return json;
    }

    private static Integer[] getMaxAndMinArray() {
        Integer[] ints = new Integer[2];
        if (additionalBlockHeightObj.get("startHeight") != null && additionalBlockHeightObj.get("endHeight") != null) {
            // todo 检测类型转换是否存在异常
            int startHeight = Integer.parseInt(String.valueOf(additionalBlockHeightObj.get("startHeight")));
            int endHeight = Integer.parseInt(String.valueOf(additionalBlockHeightObj.get("endHeight")));
            if (startHeight > Conch.getHeight() - forkBlocksLevel.MINI.getLevel()) {
                startHeight = Conch.getHeight() - forkBlocksLevel.MINI.getLevel();
            }
            if (endHeight <= Conch.getHeight()) {
                endHeight = Conch.getHeight();
            }
            ints[0] = startHeight;
            ints[1] = endHeight;
        } else {
            ints[0] = Conch.getHeight() - forkBlocksLevel.MINI.getLevel();
            ints[1] = Conch.getHeight();
        }
        additionalBlockHeightObj.clear();
        return ints;
    }

    /**
     * if sendNode not processForkNode or BootNode, set as false
     * @param state
     * @param conditionObj
     */
    private static void generateMyPeerInfoRequest(Peer.BlockchainState state, JSONObject conditionObj){
        // generate my peer details and update state
        // generate the request and response api
        if (state != currentBlockchainState) {
            JSONObject myPeerJson = generateMyPeerJson(conditionObj);
            myPeerJson.put("blockchainState", state.ordinal());
            myPeerInfoResponse = JSON.prepare(myPeerJson);

            myPeerJson.put("requestType", "getInfo");
            myPeerJson.put("bestPeer", getBestPeerUri());
            myPeerInfoRequest = JSON.prepareRequest(myPeerJson);
            currentBlockchainState = state;
        } else if (conditionObj != null && state == currentBlockchainState) {
            JSONObject myPeerJson = generateMyPeerJson(conditionObj);
            myPeerJson.put("blockchainState", state.ordinal());
            myPeerInfoResponse = JSON.prepare(myPeerJson);

            myPeerJson.put("requestType", "getInfo");
            myPeerJson.put("bestPeer", getBestPeerUri());
            myPeerInfoRequest = JSON.prepareRequest(myPeerJson);
        }
    }

    public static JSONObject generateMyPeerJson() {
        return generateMyPeerJson(null);
    }
    
    public static JSONObject generateMyPeerJson(JSONObject conditionObj){
        JSONObject json = new JSONObject(myPeerInfo);
        json.put("peerLoad", getBestPeerLoad().toJson());
        json.putAll(getBlockchainSummary());
        json.putAll(getForkBlockSummary(conditionObj));
        return json;
    }

    /**
     * check current block chain status and generate my peer info request.
     * peer info request used to tell my peer info to other peers when connected
     *
     */
    private static void checkBlockchainStateAndGenerateMyPeerInfoRequest(Boolean setToUpToDate, JSONObject conditionObj) {
        Peer.BlockchainState state = Peer.BlockchainState.LIGHT_CLIENT;
        if(!Constants.isLightClient) {
            
            boolean isObsoleted = Conch.getBlockchain().getLastBlockTimestamp() < Conch.getEpochTime() - Constants.GAP_SECONDS;
            boolean isBiggerTarget = (Conch.getBlockchain().getLastBlock().getBaseTarget() / Constants.INITIAL_BASE_TARGET) > 10;
            
            if(Conch.getBlockchainProcessor().isDownloading()){
                state = Peer.BlockchainState.DOWNLOADING;
            }else if(isObsoleted){
                state = Peer.BlockchainState.OBSOLETE;
            }else if(isBiggerTarget && Constants.isMainnet()){
                state = Peer.BlockchainState.FORK;
            }else{
                state = Peer.BlockchainState.UP_TO_DATE;
            }
            
            // force change state
            if(setToUpToDate != null 
            && setToUpToDate == true){
                state = Peer.BlockchainState.UP_TO_DATE;
            }
        }
        
        generateMyPeerInfoRequest(state, conditionObj);
    }

    public static JSONObject reqOrResObj = new JSONObject();

    public static JSONStreamAware getMyPeerInfoRequest() {
        checkBlockchainStateAndGenerateMyPeerInfoRequest(null, null);
        return myPeerInfoRequest;
    }

    public static JSONStreamAware getMyPeerInfoResponse() {
        checkBlockchainStateAndGenerateMyPeerInfoRequest(null, null);
        return myPeerInfoResponse;
    }

    public static JSONStreamAware getMyPeerInfoRequestToCollectForkNode() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sendToCollectForkNode", true);
        checkBlockchainStateAndGenerateMyPeerInfoRequest(null, jsonObject);
        return myPeerInfoRequest;
    }

    public static JSONStreamAware getMyPeerInfoResponseToProcessForkNode() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sendToProcessForkNode", true);
        checkBlockchainStateAndGenerateMyPeerInfoRequest(null, jsonObject);
        return myPeerInfoResponse;
    }

    public static JSONStreamAware getMyPeerInfoResponseToCommonNode(String bindRsAccount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sendToCommonNode", bindRsAccount);
        checkBlockchainStateAndGenerateMyPeerInfoRequest(null, jsonObject);
        return myPeerInfoResponse;
    }

    public static Peer.BlockchainState getMyBlockchainState() {
        return Constants.isOffline ? Peer.BlockchainState.UP_TO_DATE : currentBlockchainState;
    }

    public static String getMyBlockchainStateName() {
        Peer.BlockchainState state = getMyBlockchainState();
        return state != null ? state.name() : "None";
    }

    public static Peer.BlockchainState checkAndUpdateBlockchainState(Boolean forceSetToUpToDate) {
        checkBlockchainStateAndGenerateMyPeerInfoRequest(forceSetToUpToDate, null);
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

    private Peers() {
    } // never

    public static PeerLoad getBestPeerLoad() {
        return "127.0.0.1".equals(bestPeer) ? myLoad : peers.get(bestPeer).getPeerLoad();
    }

    public static String getBestPeerUri() {
        return "127.0.0.1".equals(bestPeer) ? "http://127.0.0.1:" + API.openAPIPort : peers.get(bestPeer).getPeerApiUri().toString();
    }

    public static void checkAndAddOpeningServices(List<Peer.Service> services) {
        myServices.addAll(services);
        myServices = Collections.unmodifiableList(myServices);
    }

    public static boolean isOpenService(Peer.Service service) {
        return myServices.contains(service);
    }

    public static Peer checkOrConnectBootNodeRandom(boolean needConnectNow) {
        return _connectToPeer(Constants.getBootNodeRandom(), needConnectNow);
    }

    public static List<Peer> checkOrConnectAllGuideNodes(boolean needConnectNow) {
        List<Peer> connectedNodes = Lists.newArrayList();

        boolean connectedBootNodes = false;
        List<String> needConnectNodes = Lists.newArrayList();
        for (String nodeHost : bootNodesHost) {
            if (Conch.matchMyAddress(nodeHost)) {
                continue;
            }
            if (!Guard.forceConnectToBootNode() && nodeHost.contains("boot")) {
                continue;
            }

            // reconnect to all boot nodes
            if (needConnectNow) {
                needConnectNodes.add(nodeHost);
                continue;
            }

            // check whether connect one of boot nodes
            Peer peer = Peers.getPeer(nodeHost, true);
            if (peer != null
                    && Peer.State.CONNECTED == peer.getState()) {
                connectedBootNodes = true;
                connectedNodes.add(peer);
            } else {
                needConnectNodes.add(nodeHost);
            }
        }

        // connect to none-connected boot nodes
        if(!connectedBootNodes) {
            needConnectNodes.forEach(nodeHost -> {
                Peer peer = _connectToPeer(nodeHost, needConnectNow);
                if(peer != null) {
                    connectedNodes.add(peer);
                }
            });
        }

        return connectedNodes;
    }

    private static Peer _connectToPeer(String nodeHost, boolean needConnectNow){
        Peer peer = Peers.getPeer(nodeHost, true);
        if(peer == null) {
            peer = Peers.findOrCreatePeer(nodeHost, false, true);
            if(peer != null) {
                Peers.addPeer(peer);
                needConnectNow = true;
            }
        }else if(StringUtils.isEmpty(peer.getAnnouncedAddress())
        || StringUtils.isEmpty(peer.getHost())){
            needConnectNow = true;
        }

        if(peer != null) {
            if(needConnectNow || Peer.State.CONNECTED != peer.getState()){
                if (Logger.printNow(Logger.PEERS_CHECK_OR_CONNECT_TO_PEER)) {
                    Logger.logDebugMessage("Re-connect boot node %s[%s] when its state is %s",
                            peer.getAnnouncedAddress(), peer.getHost(), peer.getState());
                }
                connectPeer(peer);
            }
        }
        return peer;
    }

    public static void checkOrReConnectAllPeers(){
        for (Peer peer : peers.values()) {
            if(peer != null && Peer.State.CONNECTED != peer.getState()) {
                connectPeer(peer);
            }
        }
        checkOrConnectAllGuideNodes(true);
    }

    /**
     * Stores data before the fork point
     */
    public static Map<Integer, JSONObject> blocksMap = Maps.newTreeMap();
    public static HashSet<Long> blockIdsSet = Sets.newHashSet();
    public static HashMap<Long, ForkBlock> blockMap = Maps.newHashMap();
    public static HashSet<ForkBlock> newBlockSet = Sets.newHashSet();
    /**
     * Used to detect missing blocks
     */
    public static Map<String, HashSet<ForkBlock.ForkBlockLinkedAccount>> blockLinkedGeneratorMap = Maps.newHashMap();
    public static Map<String, TreeSet<Integer>> blockLinkedGeneratorHeightMap = Maps.newHashMap();
    public static Set<ForkBlock.ForkBlockLinkedAccount> newBlockLinkedGeneratorSet = Sets.newHashSet();
    public static int commonBlockHeight = 0;
    public static int forkSize;
    public static int maxHeight = 0;
    public static int minHeight = 0;
    private static long lastTime = System.currentTimeMillis();
    public static Set<String> allNodeSet = Sets.newHashSet();

    /**
     * loop all forks, confirm commonBlockHeight, base on commonBlockHeight to analyze fork size and report to DingTalk
     * @param forkBlocksMapData
     */
    public static void processForkBlocksMap(Map<String, List<JSONObject>> forkBlocksMapData) {
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime > 30 * 60 * 1000) {
                commonBlockHeight = 0;
                allNodeSet.clear();
                lastTime = currentTime;
            }
            if (Generator.HUB_BIND_ADDRESS != null) {
                forkBlocksMapData.put(Generator.HUB_BIND_ADDRESS, getForkBlocks(Conch.getHeight()-forkBlocksLevel.MINI.getLevel(), Conch.getHeight()));
            }
            // confirmed commonBlockHeight
            for (Map.Entry<String, List<JSONObject>> entry : forkBlocksMapData.entrySet()) {
                allNodeSet.add(entry.getKey());
                List<JSONObject> blocks = entry.getValue();
                Collections.reverse(blocks);
                for (JSONObject currentBlock : blocks) {
                    String blockId =(String) currentBlock.get("block");
                    int height = (int) currentBlock.get("height");
                    if (blocksMap.get(height) == null) {
                        blocksMap.put(height, currentBlock);
                        if (height > maxHeight) {
                            maxHeight = height;
                        }
                        if (height < minHeight) {
                            minHeight = height;
                        }
                    } else {
                        if (!blocksMap.get(height).get("block").equals(blockId)) {
                            // detect the oldest fork point, reset every 30 minutes
                            if (commonBlockHeight != 0 && height >= commonBlockHeight) {
                                continue;
                            }
                            commonBlockHeight = (int) currentBlock.get("height");
                            lastTime = System.currentTimeMillis();
                            // Remove blocks of blocksMap with height greater than commonBlockHeight
                            for (Integer currentHeight : blocksMap.keySet()) {
                                if (currentHeight >= commonBlockHeight) {
                                    blocksMap.remove(currentHeight);
                                }
                            }
                            updateCommonBlocksMap();
                            break;
                        }
                    }
                }
            }
            if (commonBlockHeight != 0) {
                for (Map.Entry<String, List<JSONObject>> entry : forkBlocksMapData.entrySet()) {
                    String generatorRS = entry.getKey();
                    List<JSONObject> blocks = entry.getValue();
                    Collections.reverse(blocks);
                    // Collect data of each miner node
                    List<JSONObject> myBlocks = forkBlocksMapByProcessNode.get(generatorRS);
                    // Update Miner Block Data: Remove blocks before commonBlockHeight and add blocks after commonBlockHeight
                    if (myBlocks == null) {
                        myBlocks = forkBlocksMapByProcessNode.put(generatorRS, Lists.newArrayList());
                    } else {
                        myBlocks.removeIf(myBlock -> (int) myBlock.get("height") < commonBlockHeight);
                    }
                    for (JSONObject block : blocks) {
                        if ((int) block.get("height") < commonBlockHeight) {
                            continue;
                        }
                        myBlocks.add(block);
                    }
                    int currentMaxHeight = (int) myBlocks.get(myBlocks.size() - 1).get("height");
                    int currentMinHeight = (int) myBlocks.get(0).get("height");
                    if (myBlocks.size() < currentMaxHeight - currentMinHeight) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("startHeight", currentMinHeight);
                        jsonObject.put("endHeight", currentMaxHeight);
                        missingForkBlocksMap.put(generatorRS, jsonObject);
                    } else {
                        missingForkBlocksMap.remove(generatorRS);
                    }
                    forkBlocksMapByProcessNode.put(generatorRS, myBlocks);
                }
                forkSize = maxHeight - commonBlockHeight;
                if (forkSize >= 18) {
                    reportToDingTalk(commonBlockHeight);
                }
            } else {
                updateCommonBlocksMap();
            }
            // todo 记录commonBlock之前的节点
            for (Map.Entry<String, List<JSONObject>> entry : forkBlocksMapByProcessNode.entrySet()) {
                processBlocksToForkObj(entry.getKey(), entry.getValue());
            }
            // todo 3/4完成 持久化 forkBlocksMapByProcessNode节点数据

        } catch (Exception e) {
            e.printStackTrace();
            Logger.logErrorMessage("Processing ForkBlocks data failed, error: " + e);
        } finally {
            forkBlockObjMapToAPI.clear();
            forkBlockObjMapToAPI.putAll(forkBlockObjMap);
            forkBlockObjMap.clear();
        }

    }

     /**
     * loop all forks, confirm commonBlockHeight, base on commonBlockHeight to analyze fork size and report to DingTalk
     * @param forkBlocksMapData
     */
    public static void processForkBlocksMap2(Map<String, List<JSONObject>> forkBlocksMapData) {
        try {
            // Clear invalid data of database
            int minHeight = 0;
            if (maxHeight != 0) {
                minHeight = maxHeight - forkBlocksLevel.LONG.getLevel();
            }

            // Collect latest forkBlocks data of all nodes
            if (Generator.HUB_BIND_ADDRESS != null) {
                if (missingForkBlocksMap.get(Generator.HUB_BIND_ADDRESS) != null) {
                    JSONObject jsonObject = missingForkBlocksMap.get(Generator.HUB_BIND_ADDRESS);
                    int startHeight = (int) jsonObject.get("startHeight");
                    int endHeight = (int) jsonObject.get("endHeight");
                    forkBlocksMapData.put(Generator.HUB_BIND_ADDRESS, getForkBlocks(startHeight, endHeight));
                    missingForkBlocksMap.remove(Generator.HUB_BIND_ADDRESS);
                } else {
                    forkBlocksMapData.put(Generator.HUB_BIND_ADDRESS, getForkBlocks(Conch.getHeight()-forkBlocksLevel.MINI.getLevel(), Conch.getHeight()));
                }
            }
            for (Map.Entry<String, List<JSONObject>> entry : forkBlocksMapData.entrySet()) {
                String generator = entry.getKey();
                List<JSONObject> blocks = entry.getValue();
                // Check whether the node is block rolled back, if so, then discard the stored relevant data
                JSONObject latestForkBlock = blocks.get(0);
                int latestHeight =Integer.parseInt(String.valueOf(latestForkBlock.get("height")));
                TreeSet<Integer> heights = blockLinkedGeneratorHeightMap.get(generator);
                if (heights != null && latestHeight < heights.last()) {
                    // reset, for block leak detection logic
                    blockLinkedGeneratorHeightMap.put(generator, Sets.newTreeSet());
                    blockLinkedGeneratorMap.put(generator, Sets.newHashSet());
                    deleteForkBlocksAndLinkedFromAccount(generator);
                    continue;
                }
                long accountId = Account.rsAccountToId(generator);
                Collections.reverse(blocks);
                for (JSONObject currentBlock : blocks) {
                    int height = Integer.parseInt(String.valueOf(currentBlock.get("height")));
                    if (height < minHeight) {
                        continue;
                    }
                    long blockId = (long) currentBlock.get("block");
                    String generatorRS = (String) currentBlock.get("generatorRS");
                    long generatorId = Account.rsAccountToId(generatorRS);
                    int timestamp = Integer.parseInt(String.valueOf(currentBlock.get("timestamp")));
                    int version = Integer.parseInt(String.valueOf(currentBlock.get("version")));
                    String cumulativeDifficulty = (String) currentBlock.get("cumulativeDifficulty");
                    ForkBlock.ForkBlockLinkedAccount linkedAccount = new ForkBlock.ForkBlockLinkedAccount(blockId, accountId, height);
                    ForkBlock forkBlock = new ForkBlock(version, timestamp, blockId, generatorId, BigInteger.valueOf(Long.parseLong(cumulativeDifficulty)), height);
                    if (!blockIdsSet.contains(blockId)) {
                        blockIdsSet.add(blockId);
                        blockMap.put(blockId, forkBlock);
                        newBlockSet.add(forkBlock);
                        if (height > maxHeight) {
                            maxHeight = height;
                        }
                    }
                    boolean status;
                    if (blockLinkedGeneratorHeightMap.get(generator) == null) {
                        TreeSet<Integer> treeSet = Sets.newTreeSet();
                        status = treeSet.add(height);
                        HashSet<ForkBlock.ForkBlockLinkedAccount> hashSet = Sets.newHashSet();
                        hashSet.add(linkedAccount);
                        blockLinkedGeneratorMap.put(generator, hashSet);
                        blockLinkedGeneratorHeightMap.put(generator, treeSet);
                    } else {
                        TreeSet<Integer> treeSet = blockLinkedGeneratorHeightMap.get(generator);
                        HashSet<ForkBlock.ForkBlockLinkedAccount> hashSet = blockLinkedGeneratorMap.get(generator);
                        status = treeSet.add(height);
                        hashSet.add(linkedAccount);
                        blockLinkedGeneratorMap.put(generator, hashSet);
                        blockLinkedGeneratorHeightMap.put(generator, treeSet);
                    }
                    if (status) {
                        newBlockLinkedGeneratorSet.add(linkedAccount);
                    }
                }
            }

            // Data is stored in the database, and clear Map/Set
            try {
                Db.db.beginTransaction();
                PeerDb.saveForkBlocks(newBlockSet);
                PeerDb.saveForkBlockLinkedAccounts(newBlockLinkedGeneratorSet);
                Db.db.commitTransaction();
                newBlockSet.clear();
                newBlockLinkedGeneratorSet.clear();
            } catch (Exception e) {
                Logger.logErrorMessage(e.toString(), e);
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }

            // Check the data of each node and report missing blocks to the collectionForkNode
            for (Map.Entry<String, TreeSet<Integer>> entry : blockLinkedGeneratorHeightMap.entrySet()) {
                String generator = entry.getKey();
                TreeSet<Integer> value = entry.getValue();
                JSONObject jsonObject = new JSONObject();
                if (value.isEmpty()) {
                    jsonObject.put("startHeight", maxHeight - forkBlocksLevel.LONG.getLevel());
                    jsonObject.put("endHeight", maxHeight);
                    missingForkBlocksMap.put(generator, jsonObject);
                } else {
                    if (Math.abs(value.first() - value.last()) + 1 > value.size()) {
                        int cursorMin = value.first();
                        int cursorMax = value.last();
                        // Locate the specific range
                        while (value.contains(cursorMax)) {
                            cursorMax--;
                        }
                        while (value.contains(cursorMin)) {
                            cursorMin++;
                        }
                        jsonObject.put("startHeight", cursorMin);
                        jsonObject.put("endHeight", cursorMax);
                        missingForkBlocksMap.put(generator, jsonObject);
                    } else {
                        missingForkBlocksMap.remove(generator);
                    }
                }
            }

            ArrayList<Integer> linkedAllHeights = Lists.newArrayList();
            // Combine the data of all nodes and all blocks to generate fork chain info
            for (Map.Entry<String, HashSet<ForkBlock.ForkBlockLinkedAccount>> entry : blockLinkedGeneratorMap.entrySet()) {
                String generator = entry.getKey();
                HashSet<ForkBlock.ForkBlockLinkedAccount> linkedBlocks = entry.getValue();
                ArrayList<ForkBlock> forkBlockList = Lists.newArrayList();
                for (ForkBlock.ForkBlockLinkedAccount linkedBlock : linkedBlocks) {
                    forkBlockList.add(blockMap.get(linkedBlock.getBlockId()));
                    linkedAllHeights.add(linkedBlock.getHeight());
                }
                processForkBlocksToForkObj(generator, forkBlockList);
            }

            // Detect fork points
            List<Integer> duplicateElements = getDuplicateElements(linkedAllHeights);
            if (!duplicateElements.isEmpty()) {
                for (Integer element : duplicateElements) {
                    if (maxHeight - element >= 18) {
                        reportToDingTalk(element);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Logger.logErrorMessage("Processing ForkBlocks data failed, error: " + e);
        } finally {
            forkBlockObjMapToAPI.clear();
            forkBlockObjMapToAPI.putAll(forkBlockObjMap);
            forkBlockObjMap.clear();
        }

    }

    public static <E> List<E> getDuplicateElements(List<E> list) {
        return list.stream()
                .collect(Collectors.toMap(e -> e, e -> 1, Integer::sum))
                .entrySet()
                .stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static void processForkBlocksToForkObj(String generator, ArrayList<ForkBlock> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        blocks.sort(new Comparator<ForkBlock>() {
            @Override
            public int compare(ForkBlock o1, ForkBlock o2) {
                if (o1.getHeight() > o2.getHeight()) {
                    return 1;
                } else if(o1.getHeight() == o2.getHeight()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        // todo 考虑存储stringId，将blockId转为String格式，便于UI展示
        ForkBlock lastBlock = blocks.get(blocks.size() - 1);
        long blockId = lastBlock.getId();
        // new fork
        if (!forkBlockObjMap.containsKey(blockId)) {
            // loop blocks, Compare the existence of an item equal to blockId
            /*for (Map.Entry<String, ForkObj> entry : forkObjMap.entrySet()) {
                int flag = 0;
                for (JSONObject block : entry.getValue().getBlocks()) {
                    if (blockId.equals(block.get("block"))) {
                        forkObjMap.get(entry.getKey()).addGenerator(generatorRS);
                        flag = 1;
                        break;
                    }
                }
                if (flag != 0) {
                    break;
                }
            }*/
            forkBlockObjMap.put(blockId, new ForkBlock.ForkBlockObj(blockId, blocks, generator));
        } else {
            // process old fork
            if (blocks.size() > forkBlockObjMap.get(blockId).getForkBlocks().size()) {
                ForkBlock.ForkBlockObj forkBlockObj = forkBlockObjMap.get(blockId);
                // replace forkBlocks
                forkBlockObj.setForkBlocks(blocks);
                forkBlockObjMap.put(blockId, forkBlockObj);
            }
            forkBlockObjMap.get(blockId).addGenerator(generator);
        }
    }

    /**
     * clean up invalid data, and update
     * the latest block is more than 3 * 144 away from the current height
     */
    private static void updateCommonBlocksMap() {
        if (maxHeight - minHeight > forkBlocksLevel.LONG.getLevel()) {
            int startHeight = maxHeight - forkBlocksLevel.LONG.getLevel();
            for (int height : blocksMap.keySet()) {
                if (height < startHeight) {
                    blocksMap.remove(height);
                }
            }
            minHeight = startHeight;
        }
        List<JSONObject> blocks = new ArrayList<>(blocksMap.values());
        forkBlocksMapByProcessNode.put("common_" + allNodeSet.size(), blocks);
    }

     /**
     * clean up invalid data, and update
     * the latest block is more than 3 * 144 away from the current height
     */
    private static void updateBlocksMap() {
        if (maxHeight - minHeight > forkBlocksLevel.LONG.getLevel()) {
            int startHeight = maxHeight - forkBlocksLevel.LONG.getLevel();
            for (int height : blocksMap.keySet()) {
                if (height < startHeight) {
                    blocksMap.remove(height);
                }
            }
            minHeight = startHeight;
        }
        List<JSONObject> blocks = new ArrayList<>(blocksMap.values());
        forkBlocksMapByProcessNode.put("common_" + allNodeSet.size(), blocks);
    }

    /**
     * report fork info to DingTalk
     * @param height
     */
    private static void reportToDingTalk(Integer height) {

    }

    /**
     * Label different forks based on key
     * @param generatorRS
     * @param blocks
     */
    public static void processBlocksToForkObj(String generatorRS, List<JSONObject> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        JSONObject lastBlock = blocks.get(blocks.size()-1);
        String blockId =(String) lastBlock.get("block");
        // new fork
        if (!forkObjMap.containsKey(blockId)) {
            // loop blocks, Compare the existence of an item equal to blockId
            /*for (Map.Entry<String, ForkObj> entry : forkObjMap.entrySet()) {
                int flag = 0;
                for (JSONObject block : entry.getValue().getBlocks()) {
                    if (blockId.equals(block.get("block"))) {
                        forkObjMap.get(entry.getKey()).addGenerator(generatorRS);
                        flag = 1;
                        break;
                    }
                }
                if (flag != 0) {
                    break;
                }
            }*/
            forkObjMap.put(blockId, new ForkObj(blockId, blocks, generatorRS));
            return;
        }
        // process old fork
        forkObjMap.get(blockId).addGenerator(generatorRS);
    }

    /**
     * save or update any bindRsAccount forkBlocks,new blocks direct replacement of old blocks
     * @param bindRsAccount
     * @param blocks
     */
    public static void saveOrUpdateForkBlocks(String bindRsAccount, List<JSONObject> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        forkBlocksMap.put(bindRsAccount, blocks);
    }


}
