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
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.http.API;
import org.conch.http.APIEnum;
import org.conch.security.Guard;
import org.conch.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.GZIPInputStream;

final class PeerImpl implements Peer {
    // ip
    private final String host;
    private final PeerWebSocket webSocket;
    private volatile PeerWebSocket inboundSocket;
    private volatile boolean useWebSocket;
    private volatile boolean useNATService;
    // domain or full NAT address
    private volatile String announcedAddress;
    private volatile int port;
    private volatile String bindRsAccount;
    private volatile boolean shareAddress;
    private volatile Hallmark hallmark;
    private volatile String platform;
    private volatile String application;
    private volatile int apiPort;
    private volatile int apiSSLPort;
    private volatile EnumSet<APIEnum> disabledAPIs;
    private volatile int apiServerIdleTimeout;
    private volatile String version;
    private volatile boolean isOldVersion;
    private volatile long adjustedWeight;
    private volatile int blacklistingTime;
    private volatile String blacklistingCause;
    private volatile State state;
    private volatile long downloadedVolume;
    private volatile long uploadedVolume;
    private volatile int lastUpdated;
    private volatile int lastConnectAttempt;
    private volatile int lastInboundRequest;
    private volatile long hallmarkBalance = -1;
    private volatile int hallmarkBalanceHeight;
    private volatile long services;
    private volatile BlockchainState blockchainState;
    private volatile Type type;
    private volatile PeerLoad peerLoad;
    private volatile JSONObject blockSummaryJson = new JSONObject();
    private volatile String cosUpdateTime;


    PeerImpl(String host, String announcedAddress) {
        this.host = host;
        this.useNATService = false;
        this.announcedAddress = announcedAddress;
        try {
            this.port = new URI("http://" + announcedAddress).getPort();
        } catch (URISyntaxException ignore) {}
        this.state = State.NON_CONNECTED;
        this.shareAddress = true;
        this.webSocket = new PeerWebSocket();
        this.useWebSocket = Peers.useWebSockets && !Peers.useProxy;
        this.disabledAPIs = EnumSet.noneOf(APIEnum.class);
        this.apiServerIdleTimeout = API.apiServerIdleTimeout;
        this.blockchainState = BlockchainState.NONE;
        this.peerLoad = new PeerLoad(this.host, this.port, 0);
    }

    @Override
    public String getCosUpdateTime() {
        return cosUpdateTime;
    }

    public void setCosUpdateTime(String cosUpdateTime) {
        this.cosUpdateTime = cosUpdateTime;
    }

    @Override
    public boolean isUseNATService() {
        return useNATService;
    }

    public void setUseNATService(boolean useNATService) {
        this.useNATService = useNATService;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public State getState() {
        return state;
    }

    void setState(State state) {
        if (state != State.CONNECTED) {
            webSocket.close();
        }
        if (this.state == state) {
            return;
        }
        if (this.state == State.NON_CONNECTED) {
            this.state = state;
            Peers.notifyListeners(this, Peers.Event.ADDED_ACTIVE_PEER);
        } else if (state != State.NON_CONNECTED) {
            this.state = state;
            Peers.notifyListeners(this, Peers.Event.CHANGED_ACTIVE_PEER);
        } else {
            this.state = state;
        }
    }

    @Override
    public Type getType() {
        return this.type == null ? Type.NORMAL : this.type;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String getBindRsAccount() {
        return bindRsAccount;
    }

    @Override
    public void setBindRsAccount(String bindRsAccount) {
        this.bindRsAccount = bindRsAccount;
    }

    @Override
    public long getDownloadedVolume() {
        return downloadedVolume;
    }

    void updateDownloadedVolume(long volume) {
        synchronized (this) {
            downloadedVolume += volume;
        }
        Peers.notifyListeners(this, Peers.Event.DOWNLOADED_VOLUME);
    }

    @Override
    public long getUploadedVolume() {
        return uploadedVolume;
    }

    void updateUploadedVolume(long volume) {
        synchronized (this) {
            uploadedVolume += volume;
        }
        Peers.notifyListeners(this, Peers.Event.UPLOADED_VOLUME);
    }

    @Override
    public String getVersion() {
        return version;
    }

    void setVersion(String version) {
        if (version != null && version.length() > Peers.MAX_VERSION_LENGTH) {
            throw new IllegalArgumentException("Invalid version length: " + version.length());
        }
        boolean versionChanged = version == null || !version.equals(this.version);
        this.version = version;
        isOldVersion = false;
        if (Conch.APPLICATION.equals(application)) {
            isOldVersion = Peers.isOldVersion(version, Constants.MIN_VERSION);
            if (isOldVersion) {
                if (versionChanged) {
                    Logger.logDebugMessage(String.format("Blacklisting %s version %s", host, version));
                }
                blacklistingCause = "Old version: " + version;
                lastInboundRequest = 0;
                setState(State.NON_CONNECTED);
                Peers.notifyListeners(this, Peers.Event.BLACKLIST);
            }
        }
    }

    @Override
    public String getApplication() {
        return application;
    }

    void setApplication(String application) {
        if (application == null || application.length() > Peers.MAX_APPLICATION_LENGTH) {
            throw new IllegalArgumentException("Invalid application");
        }
        this.application = application;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    void setPlatform(String platform) {
        if (platform != null && platform.length() > Peers.MAX_PLATFORM_LENGTH) {
            throw new IllegalArgumentException("Invalid platform length: " + platform.length());
        }
        this.platform = platform;
    }

    @Override
    public String getSoftware() {
        return Convert.truncate(application, "?", 10, false)
                + " (" + Convert.truncate(version, "?", 10, false) + ")"
                + " @ " + Convert.truncate(platform, "?", 10, false);
    }

    @Override
    public int getApiPort() {
        return apiPort;
    }

    void setApiPort(Object apiPortValue) {
        if (apiPortValue != null) {
            try {
                apiPort = ((Long) apiPortValue).intValue();
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid peer apiPort " + apiPortValue);
            }
        }
    }

    @Override
    public int getApiSSLPort() {
        return apiSSLPort;
    }

    void setApiSSLPort(Object apiSSLPortValue) {
        if (apiSSLPortValue != null) {
            try {
                apiSSLPort = ((Long) apiSSLPortValue).intValue();
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid peer apiSSLPort " + apiSSLPortValue);
            }
        }
    }

    @Override
    public Set<APIEnum> getDisabledAPIs() {
        return Collections.unmodifiableSet(disabledAPIs);
    }

    void setDisabledAPIs(Object apiSetBase64) {
        if (apiSetBase64 instanceof String) {
            disabledAPIs = APIEnum.base64StringToEnumSet((String) apiSetBase64);
        }
    }

    @Override
    public int getApiServerIdleTimeout() {
        return apiServerIdleTimeout;
    }

    void setApiServerIdleTimeout(Object apiServerIdleTimeout) {
        if (apiServerIdleTimeout instanceof Integer) {
            this.apiServerIdleTimeout = (int) apiServerIdleTimeout;
        }
    }

    @Override
    public BlockchainState getBlockchainState() {
        return blockchainState;
    }

    void setBlockchainState(Object blockchainStateObj) {
        if (blockchainStateObj instanceof Integer) {
            int blockchainStateInt = (int) blockchainStateObj;
            if (blockchainStateInt >= 0 && blockchainStateInt < BlockchainState.values().length) {
                this.blockchainState = BlockchainState.values()[blockchainStateInt];
            }
        } else if (blockchainStateObj instanceof Long) {
            int blockchainStateInt = ((Long) blockchainStateObj).intValue();
            if (blockchainStateInt >= 0 && blockchainStateInt < BlockchainState.values().length) {
                this.blockchainState = BlockchainState.values()[blockchainStateInt];
            }
        }
    }

    @Override
    public boolean shareAddress() {
        return shareAddress;
    }

    void setShareAddress(boolean shareAddress) {
        this.shareAddress = shareAddress;
    }

    @Override
    public String getAnnouncedAddress() {
        return announcedAddress;
    }

    @Override
    public String getAddress() {
        return StringUtils.isNotEmpty(announcedAddress) ? announcedAddress : host;
    }

    void setAnnouncedAddress(String announcedAddress) {
        if (announcedAddress != null && announcedAddress.length() > Peers.MAX_ANNOUNCED_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Announced address too long: " + announcedAddress.length());
        }
        this.announcedAddress = announcedAddress;
        if (announcedAddress != null) {
            try {
                this.port = new URI("http://" + announcedAddress).getPort();
            } catch (URISyntaxException e) {
                this.port = -1;
            }
        } else {
            this.port = -1;
        }
    }

    @Override
    public int getPort() {
        return port <= 0 ? Conch.getPeerPort() : port;
    }

    @Override
    public Hallmark getHallmark() {
        return hallmark;
    }

    @Override
    public int getWeight() {
        if (hallmark == null) {
            return 0;
        }
        if (hallmarkBalance == -1 || hallmarkBalanceHeight < Conch.getBlockchain().getHeight() - 60) {
            long accountId = hallmark.getAccountId();
            Account account = Account.getAccount(accountId);
            hallmarkBalance = account == null ? 0 : account.getBalanceNQT();
            hallmarkBalanceHeight = Conch.getBlockchain().getHeight();
        }
        return (int) (adjustedWeight * (hallmarkBalance / Constants.ONE_SS) / Constants.MAX_BALANCE_SS);
    }

    @Override
    public boolean isBlacklisted() {
        return blacklistingTime > 0 || isOldVersion || Peers.knownBlacklistedPeers.contains(host)
                || (announcedAddress != null && Peers.knownBlacklistedPeers.contains(announcedAddress));
    }

    @Override
    public void blacklist(Exception cause) {
        if (cause instanceof ConchException.NotCurrentlyValidException || cause instanceof BlockchainProcessor.BlockOutOfOrderException
                || cause instanceof SQLException || cause.getCause() instanceof SQLException) {
            // don't blacklist peers just because a feature is not yet enabled, or because of database timeouts
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        if (cause instanceof ParseException && Errors.END_OF_FILE.equals(cause.toString())) {
            return;
        }
        if (!isBlacklisted()) {
            cause.printStackTrace();
            if (cause instanceof IOException || cause instanceof ParseException || cause instanceof IllegalArgumentException) {
                Logger.logDebugMessage("Blacklisting " + host + " because of: " + cause.toString());
            } else {
                Logger.logDebugMessage("Blacklisting " + host + " because of: " + cause.toString(), cause);
            }
        }
        blacklist(cause.toString() == null || Peers.hideErrorDetails ? cause.getClass().getName() : cause.toString());
    }

    boolean isProtectPeer() {
        if (!IpUtil.isFoundationDomain(this.announcedAddress)
                && !IpUtil.isFoundationDomain(this.host)) {
            return false;
        }

        for (SharderGenesis.GenesisPeer genesisPeer : SharderGenesis.GenesisPeer.getAll()) {
            if (IpUtil.matchHost(genesisPeer.domain, this.host)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void blacklist(String cause) {
        if (isProtectPeer()) {
            Logger.logDebugMessage("peer %s[%s] is the protected peer, don't black it", announcedAddress, host);
            return;
        }

        blacklistingTime = Conch.getEpochTime();
        blacklistingCause = cause;
        setState(State.NON_CONNECTED);
        lastInboundRequest = 0;
        Peers.notifyListeners(this, Peers.Event.BLACKLIST);
    }

    @Override
    public void unBlacklist() {
        if (blacklistingTime == 0) {
            return;
        }

        Logger.logDebugMessage("Unblacklisting " + host);
        setState(State.NON_CONNECTED);
        blacklistingTime = 0;
        blacklistingCause = null;
        Peers.notifyListeners(this, Peers.Event.UNBLACKLIST);
    }

    boolean updateBlacklistedStatus(int curTime) {
        if (blacklistingTime > 0
                && blacklistingTime + Peers.blacklistingPeriod <= curTime) {
            unBlacklist();
            return true;
        }
        if (isOldVersion
                && lastUpdated < curTime - 3600) {
            isOldVersion = false;
        }
        return false;
    }

    @Override
    public void deactivate() {
        setState(state == State.CONNECTED ? State.DISCONNECTED : State.NON_CONNECTED);
        Peers.notifyListeners(this, Peers.Event.DEACTIVATE);
    }

    @Override
    public void remove() {
        webSocket.close();
        Peers.removePeer(this);
        Peers.notifyListeners(this, Peers.Event.REMOVE);
    }

    @Override
    public int getLastUpdated() {
        return lastUpdated;
    }

    void setLastUpdated(int lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean isInbound() {
        return lastInboundRequest != 0;
    }

    int getLastInboundRequest() {
        return lastInboundRequest;
    }

    void setLastInboundRequest(int now) {
        lastInboundRequest = now;
    }

    void setInboundWebSocket(PeerWebSocket inboundSocket) {
        this.inboundSocket = inboundSocket;
    }

    @Override
    public boolean isInboundWebSocket() {
        PeerWebSocket s;
        return ((s = inboundSocket) != null && s.isOpen());
    }

    @Override
    public boolean isOutboundWebSocket() {
        return webSocket.isOpen();
    }

    @Override
    public String getBlacklistingCause() {
        return blacklistingCause == null ? "unknown" : blacklistingCause;
    }

    @Override
    public int getLastConnectAttempt() {
        return lastConnectAttempt;
    }

    @Override
    public boolean isType(Type type) {
        if (type == null) return false;

        return type == this.type;
    }

    @Override
    public JSONObject send(JSONStreamAware request) {
        return send(request, Peers.MAX_RESPONSE_SIZE);
    }

    @Override
    public JSONObject send(JSONStreamAware request, int maxResponseSize) {
        JSONObject response = null;
        String log = "";
        boolean showLog = Logger.isLevel(Logger.Level.DEBUG);
        HttpURLConnection connection = null;
        int communicationLoggingMask = Peers.communicationLoggingMask;

        try {

            JSONObject jsonObject = Guard.isSelfClosingPeer(this.getHost());
            if ((Boolean) jsonObject.get(Guard.KEY_NEED_CLOSING)) {
                return jsonObject;
            }

            //
            // Create a new WebSocket session if we don't have one
            //
            // [NAT] If the node use NAT, seperate the host(host like 116.89.251.206:10415) to combine the websocket url
            if (useWebSocket && !webSocket.isOpen()) {
                useWebSocket =
                        webSocket.startClient(URI.create("ws://" + Peers.addressHost(host) + ":" + Peers.addressPort(host) + "/sharder"));
            }
            //
            // Send the request and process the response
            //
            if (useWebSocket) {
                //
                // Send the request using the WebSocket session
                //
                StringWriter wsWriter = new StringWriter(1000);
                request.writeJSONString(wsWriter);
                String wsRequest = wsWriter.toString();
                if (communicationLoggingMask != 0) {
                    log = "WebSocket " + host + ": " + wsRequest;
                }
                String wsResponse = webSocket.doPost(wsRequest);
                updateUploadedVolume(wsRequest.length());
                if (maxResponseSize > 0) {
                    if ((communicationLoggingMask & Peers.LOGGING_MASK_200_RESPONSES) != 0) {
                        log += " >>> " + wsResponse;
                        showLog = true;
                    }
                    if (wsResponse.length() > maxResponseSize)
                        throw new ConchException.ConchIOException("Maximum size exceeded: " + wsResponse.length());
                    response = (JSONObject) JSONValue.parseWithException(wsResponse);
                    updateDownloadedVolume(wsResponse.length());
                }
            } else {
                //
                // Send the request using HTTP
                //
                URL url = new URL("http://" + Peers.addressHost(host) + ":" + Peers.addressPort(host) + "/sharder");
                if (communicationLoggingMask != 0) {
                    log += "\"" + url.toString() + "\": " + JSON.toString(request);
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(Peers.connectTimeout);
                connection.setReadTimeout(Peers.readTimeout);
                connection.setRequestProperty("Accept-Encoding", "gzip");
                connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"))) {
                    CountingOutputWriter cow = new CountingOutputWriter(writer);
                    request.writeJSONString(cow);
                    updateUploadedVolume(cow.getCount());
                }
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    if (maxResponseSize > 0) {
                        if ((communicationLoggingMask & Peers.LOGGING_MASK_200_RESPONSES) != 0) {
                            CountingInputStream cis = new CountingInputStream(connection.getInputStream(), maxResponseSize);
                            InputStream responseStream = cis;
                            if ("gzip".equals(connection.getHeaderField("Content-Encoding"))) {
                                responseStream = new GZIPInputStream(cis);
                            }
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int numberOfBytes;
                            try (InputStream inputStream = responseStream) {
                                while ((numberOfBytes = inputStream.read(buffer, 0, buffer.length)) > 0) {
                                    byteArrayOutputStream.write(buffer, 0, numberOfBytes);
                                }
                            }
                            String responseValue = byteArrayOutputStream.toString("UTF-8");
                            if (responseValue.length() > 0 && responseStream instanceof GZIPInputStream) {
                                log += String.format("[length: %d, compression ratio: %.2f]",
                                        cis.getCount(), (double) cis.getCount() / (double) responseValue.length());
                            }
                            log += " >>> " + responseValue;
                            showLog = true;
                            response = (JSONObject) JSONValue.parseWithException(responseValue);
                            updateDownloadedVolume(responseValue.length());
                        } else {
                            InputStream responseStream = connection.getInputStream();
                            if ("gzip".equals(connection.getHeaderField("Content-Encoding"))) {
                                responseStream = new GZIPInputStream(responseStream);
                            }
                            try (Reader reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"))) {
                                CountingInputReader cir = new CountingInputReader(reader, maxResponseSize);
                                response = (JSONObject) JSONValue.parseWithException(cir);
                                updateDownloadedVolume(cir.getCount());
                            }
                        }
                    }
                } else {
                    if ((communicationLoggingMask & Peers.LOGGING_MASK_NON200_RESPONSES) != 0) {
                        log += " >>> Peer responded with HTTP " + connection.getResponseCode() + " code!";
                        showLog = true;
                    }
                    Logger.logDebugMessage("Peer " + host + " responded with HTTP " + connection.getResponseCode());
                    deactivate();
                    connection.disconnect();
                }
            }
            //
            // Check for an error response
            //
            if (response != null && response.get("error") != null) {
                deactivate();
                if (Errors.SEQUENCE_ERROR.equals(response.get("error")) && request != Peers.getMyPeerInfoRequest()) {
                    Logger.logDebugMessage("Sequence error, reconnecting to " + host);
                    connect();
                } else {
                    String errorReason = String.format("Peer %s version %s returned error: %s", host, version,
                            response.toJSONString());
                    Logger.logDebugMessage("%s, request was: %s, disconnecting", errorReason, JSON.toString(request));
                    if (connection != null) {
                        connection.disconnect();
                    }
                    /**
                     * 新增逻辑处理
                     *  1. 若返回的信息是：对方将我加入黑名单后，将该节点放入「自闭列表」
                     *  2. 处于自闭列表期间不再主动与该节点进行通信，有效降低对方节点的网络消耗
                     *  see {@link org.conch.security.Guard#defense(java.lang.String)}
                     */

                    Guard.updateSelfClosingPeer(host, errorReason);
                }
            }
        } catch (ConchException.ConchIOException e) {
            blacklist(e);
            if (connection != null) {
                connection.disconnect();
            }
        } catch (RuntimeException | ParseException | IOException e) {
            if (!(e instanceof UnknownHostException || e instanceof SocketTimeoutException ||
                    e instanceof SocketException || Errors.END_OF_FILE.equals(e.getMessage()))) {
                Logger.logDebugMessage(String.format("Error sending request to peer %s: %s",
                        host, e.getMessage() != null ? e.getMessage() : e.toString()));
            }
            if ((communicationLoggingMask & Peers.LOGGING_MASK_EXCEPTIONS) != 0) {
                log += " >>> " + e.toString();
                showLog = true;
            }
            deactivate();
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (showLog && StringUtils.isNotEmpty(log)) {
            Logger.logMessage(log + "\n");
        }

        return response;
    }

    @Override
    public int compareTo(Peer o) {
        if (getWeight() > o.getWeight()) {
            return -1;
        } else if (getWeight() < o.getWeight()) {
            return 1;
        }
        return getHost().compareTo(o.getHost());
    }

    void connect() {
        lastConnectAttempt = Conch.getEpochTime();
        try {
            if (!Peers.ignorePeerAnnouncedAddress && announcedAddress != null) {
                try {
                    URI uri = new URI("http://" + announcedAddress);
                    InetAddress inetAddress = InetAddress.getByName(uri.getHost());
                    // [?NAT] inetAddress contains port info, so make sure
//                    if (!inetAddress.equals(InetAddress.getByName(new URI("http://" + host).getHost()))) {
                    if (!inetAddress.equals(InetAddress.getByName(Peers.addressHost(host)))) {
                        Logger.logDebugMessage("Connect: announced address " + announcedAddress + " now points to " + inetAddress.getHostAddress() + ", replacing peer " + host);
                        Peers.removePeer(this);
                        PeerImpl newPeer = Peers.findOrCreatePeer(inetAddress, announcedAddress, useNATService, true);
                        if (newPeer != null) {
                            Peers.addPeer(newPeer);
                            newPeer.connect();
                        }
                        return;
                    }
                } catch (URISyntaxException | UnknownHostException e) {
                    blacklist(e);
                    return;
                }
            }
            // get peer detail
            JSONObject response = send(Peers.getMyPeerInfoRequest());
            if (response != null) {
                if (response.get("error") != null) {
                    setState(State.NON_CONNECTED);
                    return;
                }
                long origServices = services;
                parseJSONObject(response);

                if (!Peers.ignorePeerAnnouncedAddress) {
                    String newAnnouncedAddress = Convert.emptyToNull((String) response.get("announcedAddress"));
                    if (newAnnouncedAddress != null) {
                        newAnnouncedAddress = Peers.addressWithPort(newAnnouncedAddress.toLowerCase());
                        if (newAnnouncedAddress != null) {
                            if (!verifyAnnouncedAddress(newAnnouncedAddress)) {
                                Logger.logDebugMessage("Connect: new announced address for " + host + " not accepted");
                                if (!verifyAnnouncedAddress(announcedAddress)) {
                                    Logger.logDebugMessage("Connect: old announced address for " + host + " no longer valid");
                                    Peers.setAnnouncedAddress(this, host);
                                }
                                setState(State.NON_CONNECTED);
                                return;
                            }
                            if (!newAnnouncedAddress.equals(announcedAddress)) {
                                Logger.logDebugMessage("Connect: peer " + host + " has new announced address " + newAnnouncedAddress + ", old is " + announcedAddress);
                                int oldPort = getPort();
                                Peers.setAnnouncedAddress(this, newAnnouncedAddress);
                                if (getPort() != oldPort) {
                                    // force checking connectivity to new announced port
                                    setState(State.NON_CONNECTED);
                                    return;
                                }
                            }
                        }
                    } else {
                        Peers.setAnnouncedAddress(this, host);
                    }
                }

                if (announcedAddress == null) {
                    if (hallmark == null || hallmark.getPort() == Conch.getPeerPort()) {
                        Peers.setAnnouncedAddress(this, host);
                        Logger.logDebugMessage("Connected to peer without announced address, setting to " + host);
                    } else {
                        setState(State.NON_CONNECTED);
                        return;
                    }
                }

                if (!isOldVersion) {
                    setState(State.CONNECTED);
                    if (services != origServices) {
                        Peers.notifyListeners(this, Peers.Event.CHANGED_SERVICES);
                    }
                } else if (!isBlacklisted()) {
                    blacklist("Old version: " + version);
                }
            } else {
                Logger.logDebugMessage("Failed to connect to peer %s[%s]", announcedAddress, host);
                setState(State.NON_CONNECTED);
            }
        } catch (RuntimeException e) {
            blacklist(e);
        } finally {
            // services & [num] should be synchronized to the new Service code(max one)
            if (this.state == State.CONNECTED && ((services & 64) == 64)) {
                if (peerLoad.getLoad() < Peers.getBestPeerLoad().getLoad()) {
                    Peers.setBestPeer(host);
                }
            }
        }
    }

    boolean verifyAnnouncedAddress(String newAnnouncedAddress) {
        if (newAnnouncedAddress == null) {
            return true;
        }
        // [NAT] adapt for NAT announcedAddress
        if (useNATService) {
            return true;
        }
        try {
            URI uri = new URI("http://" + newAnnouncedAddress);
            int announcedPort = uri.getPort() == -1 ? Conch.getPeerPort() : uri.getPort();
            if (hallmark != null && announcedPort != hallmark.getPort()) {
                Logger.logDebugMessage("Announced port " + announcedPort + " does not match hallmark " + hallmark.getPort() + ", ignoring hallmark for " + host);
                unsetHallmark();
            }
            InetAddress address = InetAddress.getByName(Peers.addressHost(host));
            for (InetAddress inetAddress : InetAddress.getAllByName(uri.getHost())) {
                if (inetAddress.equals(address)) {
                    return true;
                }
            }
            Logger.logDebugMessage("Announced address " + newAnnouncedAddress + " does not resolve to " + host);
        } catch (UnknownHostException | URISyntaxException e) {
            Logger.logDebugMessage(e.toString());
            blacklist(e);
        }
        return false;
    }

    void parsePeerLoad(JSONObject peerLoad) {
        this.peerLoad.setState(State.CONNECTED);
        this.peerLoad.setHost(host);
        this.peerLoad.setPort(apiPort);
        this.peerLoad.setLoad(Integer.parseInt(String.valueOf(peerLoad.get("load"))));
        this.peerLoad.setUri("http://" + host + ":" + apiPort);
    }

    boolean analyzeHallmark(final String hallmarkString) {
        if (Constants.isLightClient) {
            return true;
        }

        if (hallmarkString == null && this.hallmark == null) {
            return true;
        }

        if (this.hallmark != null && this.hallmark.getHallmarkString().equals(hallmarkString)) {
            return true;
        }

        if (hallmarkString == null) {
            unsetHallmark();
            return true;
        }

        try {

            Hallmark hallmark = Hallmark.parseHallmark(hallmarkString);
            if (!hallmark.isValid()) {
                Logger.logDebugMessage("Invalid hallmark " + hallmarkString + " for " + host);
                unsetHallmark();
                return false;
            }
            if (!hallmark.getHost().equals(host)) {
                InetAddress hostAddress = InetAddress.getByName(host);
                boolean validHost = false;
                for (InetAddress nextHallmark : InetAddress.getAllByName(hallmark.getHost())) {
                    if (hostAddress.equals(nextHallmark)) {
                        validHost = true;
                        break;
                    }
                }
                if (!validHost) {
                    Logger.logDebugMessage("Hallmark host " + hallmark.getHost() + " doesn't match " + host);
                    unsetHallmark();
                    return false;
                }
            }
            setHallmark(hallmark);
            long accountId = Account.getId(hallmark.getPublicKey());
            List<PeerImpl> groupedPeers = new ArrayList<>();
            int mostRecentDate = 0;
            long totalWeight = 0;
            for (PeerImpl peer : Peers.allPeers) {
                if (peer.hallmark == null) {
                    continue;
                }
                if (accountId == peer.hallmark.getAccountId()) {
                    groupedPeers.add(peer);
                    if (peer.hallmark.getDate() > mostRecentDate) {
                        mostRecentDate = peer.hallmark.getDate();
                        totalWeight = peer.getHallmarkWeight(mostRecentDate);
                    } else {
                        totalWeight += peer.getHallmarkWeight(mostRecentDate);
                    }
                }
            }

            for (PeerImpl peer : groupedPeers) {
                peer.adjustedWeight = Constants.MAX_BALANCE_SS * peer.getHallmarkWeight(mostRecentDate) / totalWeight;
                Peers.notifyListeners(peer, Peers.Event.WEIGHT);
            }

            return true;

        } catch (UnknownHostException ignore) {
        } catch (RuntimeException e) {
            Logger.logDebugMessage("Failed to analyze hallmark for peer " + host + ", " + e.toString(), e);
        }
        unsetHallmark();
        return false;

    }

    private int getHallmarkWeight(int date) {
        if (hallmark == null || !hallmark.isValid() || hallmark.getDate() != date) {
            return 0;
        }
        return hallmark.getWeight();
    }

    private void unsetHallmark() {
        removeService(Service.HALLMARK, false);
        this.hallmark = null;
    }

    private void setHallmark(Hallmark hallmark) {
        this.hallmark = hallmark;
        addService(Service.HALLMARK, false);
    }

    private void addService(Service service, boolean doNotify) {
        boolean notifyListeners;
        synchronized (this) {
            notifyListeners = ((services & service.getCode()) == 0);
            services |= service.getCode();
        }
        if (notifyListeners && doNotify) {
            Peers.notifyListeners(this, Peers.Event.CHANGED_SERVICES);
        }
    }

    private void removeService(Service service, boolean doNotify) {
        boolean notifyListeners;
        synchronized (this) {
            notifyListeners = ((services & service.getCode()) != 0);
            services &= (~service.getCode());
        }
        if (notifyListeners && doNotify) {
            Peers.notifyListeners(this, Peers.Event.CHANGED_SERVICES);
        }
    }

    long getServices() {
        synchronized (this) {
            return services;
        }
    }

    void setServices(long services) {
        synchronized (this) {
            this.services = services;
        }
    }

    @Override
    public boolean providesService(Service service) {
        boolean isProvided;
        synchronized (this) {
            isProvided = ((services & service.getCode()) != 0);
        }
        return isProvided;
    }

    @Override
    public boolean providesServices(long services) {
        boolean isProvided;
        synchronized (this) {
            isProvided = (services & this.services) == services;
        }
        return isProvided;
    }

    @Override
    public boolean isOpenAPI() {
        return providesService(Peer.Service.API) || providesService(Peer.Service.API_SSL);
    }

    @Override
    public boolean isApiConnectable() {
        return isOpenAPI() && state == Peer.State.CONNECTED
                && !Peers.isOldVersion(version, Constants.MIN_PROXY_VERSION)
                && !Peers.isNewVersion(version)
                && blockchainState == Peer.BlockchainState.UP_TO_DATE;
    }

    @Override
    public StringBuilder getPeerApiUri() {
        StringBuilder uri = new StringBuilder();
        if (providesService(Peer.Service.API_SSL)) {
            uri.append("https://");
        } else {
            uri.append("http://");
        }
        uri.append(host).append(":");
        if (providesService(Peer.Service.API_SSL)) {
            uri.append(apiSSLPort);
        } else {
            uri.append(apiPort);
        }
        return uri;
    }

    @Override
    public PeerLoad getPeerLoad() {
        return peerLoad;
    }

    @Override
    public JSONObject getBlockSummary() {
        return blockSummaryJson;
    }

    public void setPeerLoad(PeerLoad peerLoad) {
        this.peerLoad = peerLoad;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "state=" + state +
                ", useNATService='" + useNATService + '\'' +
                ", announcedAddress='" + announcedAddress + '\'' +
                ", services=" + services +
                ", host='" + host + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("useNATService", useNATService);
        json.put("announcedAddress", announcedAddress);
        json.put("hallmark", hallmark);
        json.put("application", application);
        json.put("version", version);
        json.put("platform", platform);
        json.put("shareAddress", shareAddress);
        json.put("apiPort", apiPort);
        json.put("apiSSLPort", apiSSLPort);
        json.put("disabledAPIs", disabledAPIs);
        json.put("apiServerIdleTimeout", apiServerIdleTimeout);
        json.put("services", Long.toUnsignedString(services));
        json.put("blockchainState", state.ordinal());
        json.put("peerLoad", peerLoad.toJson());
        json.put("bindRsAccount", bindRsAccount);
        json.putAll(blockSummaryJson);
        return json;
    }

    public PeerImpl parseJSONObject(JSONObject json) {
        String servicesString = (String) json.get("services");
        services = (servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);
        setApplication((String) json.get("application"));
        apiPort = Integer.parseInt(json.get("apiPort") == null ? "0" : String.valueOf(json.get("apiPort")));
        setApiSSLPort(json.get("apiSSLPort"));
        setDisabledAPIs(json.get("disabledAPIs"));
        setApiServerIdleTimeout(json.get("apiServerIdleTimeout"));
        setBlockchainState(json.get("blockchainState"));
        setCosUpdateTime(Convert.emptyToNull((String) json.get("cosUpdateTime")));
        lastUpdated = lastConnectAttempt;
        setVersion((String) json.get("version"));
        setPlatform((String) json.get("platform"));
        shareAddress = Boolean.TRUE.equals(json.get("shareAddress"));
        analyzeHallmark((String) json.get("hallmark"));
        parsePeerLoad((JSONObject) json.get("peerLoad"));
        setBindRsAccount((String) json.get("bindRsAccount"));
        blockSummaryJson.put("cumulativeDifficulty", json.get("cumulativeDifficulty"));
        blockSummaryJson.put("lastBlockHeight", json.get("lastBlockHeight"));
        blockSummaryJson.put("lastBlockId", json.get("lastBlockId"));
        blockSummaryJson.put("lastBlockHash", json.get("lastBlockHash"));
        blockSummaryJson.put("lastBlockGenerator", json.get("lastBlockGenerator"));
        blockSummaryJson.put("lastBlockTimestamp", json.get("lastBlockTimestamp"));
        blockSummaryJson.put("currentFork", json.get("currentFork"));

        return this;
    }
}
