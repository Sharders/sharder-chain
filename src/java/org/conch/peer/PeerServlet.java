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

import org.conch.BlockchainProcessor;
import org.conch.Constants;
import org.conch.Conch;
import org.conch.util.CountingInputReader;
import org.conch.util.CountingOutputWriter;
import org.conch.util.JSON;
import org.conch.util.Logger;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.sharder.util.Https;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PeerServlet extends WebSocketServlet {

    abstract static class PeerRequestHandler {
        abstract JSONStreamAware processRequest(JSONObject request, Peer peer);
        abstract boolean rejectWhileDownloading();
    }

    private static final Map<String,PeerRequestHandler> peerRequestHandlers;

    static {
        Map<String,PeerRequestHandler> map = new HashMap<>();
        map.put("addPeers", AddPeers.instance);
        map.put("getCumulativeDifficulty", GetCumulativeDifficulty.instance);
        map.put("getInfo", GetInfo.instance);
        map.put("getMilestoneBlockIds", GetMilestoneBlockIds.instance);
        map.put("getNextBlockIds", GetNextBlockIds.instance);
        map.put("getNextBlocks", GetNextBlocks.instance);
        map.put("getPeers", GetPeers.instance);
        map.put("getTransactions", GetTransactions.instance);
        map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
        map.put("processBlock", ProcessBlock.instance);
        map.put("processTransactions", ProcessTransactions.instance);
        peerRequestHandlers = Collections.unmodifiableMap(map);
    }

    static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_REQUEST_TYPE);
        UNSUPPORTED_REQUEST_TYPE = JSON.prepare(response);
    }

    private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_PROTOCOL);
        UNSUPPORTED_PROTOCOL = JSON.prepare(response);
    }

    private static final JSONStreamAware UNKNOWN_PEER;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNKNOWN_PEER);
        UNKNOWN_PEER = JSON.prepare(response);
    }

    private static final JSONStreamAware SEQUENCE_ERROR;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.SEQUENCE_ERROR);
        SEQUENCE_ERROR = JSON.prepare(response);
    }

    private static final JSONStreamAware MAX_INBOUND_CONNECTIONS;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.MAX_INBOUND_CONNECTIONS);
        MAX_INBOUND_CONNECTIONS = JSON.prepare(response);
    }

    private static final JSONStreamAware DOWNLOADING;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.DOWNLOADING);
        DOWNLOADING = JSON.prepare(response);
    }

    private static final JSONStreamAware LIGHT_CLIENT;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.LIGHT_CLIENT);
        LIGHT_CLIENT = JSON.prepare(response);
    }

    private static final BlockchainProcessor blockchainProcessor = Conch.getBlockchainProcessor();

    static JSONStreamAware error(Exception e) {
        JSONObject response = new JSONObject();
        response.put("error", Peers.hideErrorDetails ? e.getClass().getName() : e.toString());
        return response;
    }

    /**
     * Configure the WebSocket factory
     *
     * @param   factory             WebSocket factory
     */
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(Peers.webSocketIdleTimeout);
        factory.getPolicy().setMaxBinaryMessageSize(Peers.MAX_MESSAGE_SIZE);
        factory.setCreator(new PeerSocketCreator());
    }

    /**
     * Process HTTP POST request
     *
     * @param   req                 HTTP request
     * @param   resp                HTTP response
     * @throws  ServletException    Servlet processing error
     * @throws  IOException         I/O error
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONStreamAware jsonResponse;
        //
        // Process the peer request
        //
        PeerImpl peer = null;
        // [NAT] if requester is use NAT then create peer with announcedAddress instead of real remote address
        Reader bufferedReader = req.getReader(); //avoid repeatedly parsing the same Reader.
        JSONObject request = null;
        try (CountingInputReader cr = new CountingInputReader(bufferedReader, Peers.MAX_REQUEST_SIZE)) {
            request = (JSONObject) JSONValue.parseWithException(cr);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        boolean requestFromNATServer = (boolean)request.get("useNATService");
        if (requestFromNATServer) {
            peer = Peers.findOrCreatePeer((String)request.get("announcedAddress"), true);
        } else {
            peer = Peers.findOrCreatePeer(req.getRemoteAddr(), false);
        }
        if (peer == null) {
            jsonResponse = UNKNOWN_PEER;
        } else {
            jsonResponse = process(peer, bufferedReader);
        }
        //
        // Return the response
        //
        resp.setContentType("text/plain; charset=UTF-8");
        try (CountingOutputWriter writer = new CountingOutputWriter(resp.getWriter())) {
            JSON.writeJSONString(jsonResponse, writer);
            if (peer != null) {
                peer.updateUploadedVolume(writer.getCount());
            }
        } catch (RuntimeException | IOException e) {
            if (peer != null) {
                if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_EXCEPTIONS) != 0) {
                    if (e instanceof RuntimeException) {
                        Logger.logDebugMessage("Error sending response to peer " + peer.getHost(), e);
                    } else {
                        Logger.logDebugMessage(String.format("Error sending response to peer %s: %s",
                            peer.getHost(), e.getMessage() != null ? e.getMessage() : e.toString()));
                    }
                }
                peer.blacklist(e);
            }
            throw e;
        }
    }

    /**
     * Process WebSocket POST request
     *
     * @param   webSocket           WebSocket for the connection
     * @param   requestId           Request identifier
     * @param   request             Request message
     */
    void doPost(PeerWebSocket webSocket, long requestId, String request) {
        JSONStreamAware jsonResponse;
        StringReader stringReader = new StringReader(request);
        PeerImpl peer = null;
        //
        // Process the peer request
        //

        // [NAT] if requester is use NAT then create peer with announcedAddress instead of real remote address
        JSONObject requestJson = null;
        boolean requestFromNATServer = false;
        try (CountingInputReader cr = new CountingInputReader(stringReader , Peers.MAX_REQUEST_SIZE)) {
            requestJson = (JSONObject) JSONValue.parseWithException(cr);
            if (requestJson.get("useNATService") ==null) {
                Logger.logDebugMessage("useNATService == null");
                Logger.logDebugMessage(requestJson.toJSONString());
            } else {
                requestFromNATServer = (boolean) requestJson.get("useNATService");
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (requestFromNATServer) {
            peer = Peers.findOrCreatePeer((String)requestJson.get("announcedAddress"), requestFromNATServer);
        } else {
            InetSocketAddress socketAddress = webSocket.getRemoteAddress();
            if (!Peers.isUseNATService() && socketAddress == null) {
                return;
            } else {
                String remoteAddress = Peers.isUseNATService()?(String)requestJson.get("announcedAddress"):socketAddress.getHostString();
                peer = Peers.findOrCreatePeer(remoteAddress, false);
            }
        }

        if (peer == null) {
            jsonResponse = UNKNOWN_PEER;
        } else {
            peer.setInboundWebSocket(webSocket);
            jsonResponse = process(peer, new StringReader(request));
        }
        //
        // Return the response
        //
        try {
            StringWriter writer = new StringWriter(1000);
            JSON.writeJSONString(jsonResponse, writer);
            String response = writer.toString();
            webSocket.sendResponse(requestId, response);
            if (peer != null) {
                peer.updateUploadedVolume(response.length());
            }
        } catch (RuntimeException | IOException e) {
            if (peer != null) {
                if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_EXCEPTIONS) != 0) {
                    if (e instanceof RuntimeException) {
                        Logger.logDebugMessage("Error sending response to peer " + peer.getHost(), e);
                    } else {
                        Logger.logDebugMessage(String.format("Error sending response to peer %s: %s",
                            peer.getHost(), e.getMessage() != null ? e.getMessage() : e.toString()));
                    }
                }
                peer.blacklist(e);
            }
        }
    }

    /**
     * Process the peer request
     *
     * @param   peer                Peer
     * @param   inputReader         Input reader
     * @return                      JSON response
     */
    private JSONStreamAware process(PeerImpl peer, Reader inputReader) {
        //
        // Check for blacklisted peer
        //
        if (peer.isBlacklisted()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", Errors.BLACKLISTED);
            jsonObject.put("cause", peer.getBlacklistingCause());
            return jsonObject;
        }

        //
        // Process the request
        //
        try (CountingInputReader cr = new CountingInputReader(inputReader, Peers.MAX_REQUEST_SIZE)) {
            JSONObject request = (JSONObject)JSONValue.parseWithException(cr);
            //
            //network isolation
            //
//            String requestType = (String)request.get("requestType");
//            if("getInfo".equals(requestType) || "addPeers".equals(requestType)){
//                if(Peers.getPeer(peer.getHost()) == null){
//                    String url = Conch.getStringProperty("sharder.authenticationServer");
//                    url = url + peer.getHost();
//                    String responseValue = Https.httpRequest(url,"GET",null);
//                    Logger.logInfoMessage(peer.getHost() + " try to join in the network ,the serve response " + responseValue);
//                    if(responseValue == null){
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("error", Errors.CONNECTERROR);
//                        jsonObject.put("cause", "error with connecting to the serve,please try it later");
//                        return jsonObject;
//                    }
//                    if("".equals(responseValue)){
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("error", Errors.UNAUTHORIZED);
//                        jsonObject.put("cause", peer.getType());
//                        return jsonObject;
//                    }
//                    JSONObject response = (JSONObject) JSONValue.parseWithException(responseValue);
//                    String host = (String)response.get("address");
//                    Long type = (Long) response.get("type");
//                    if(peer.getHost().equals(host)){
//                        peer.setType(new Long(type).intValue());
//                    }
//                }
//            }
            Peers.addPeer(peer);

            peer.updateDownloadedVolume(cr.getCount());
            if (request.get("protocol") == null || ((Number)request.get("protocol")).intValue() != 1) {
                Logger.logDebugMessage("Unsupported protocol "

                        + request.get("protocol"));
                return UNSUPPORTED_PROTOCOL;
            }
            PeerRequestHandler peerRequestHandler = peerRequestHandlers.get((String)request.get("requestType"));
            if (peerRequestHandler == null) {
                return UNSUPPORTED_REQUEST_TYPE;
            }
            if (peer.getState() == Peer.State.DISCONNECTED) {
                peer.setState(Peer.State.CONNECTED);
            }
            if (peer.getVersion() == null && !"getInfo".equals(request.get("requestType"))) {
                return SEQUENCE_ERROR;
            }
            if (!peer.isInbound()) {
                if (Peers.hasTooManyInboundPeers()) {
                    return MAX_INBOUND_CONNECTIONS;
                }
                Peers.notifyListeners(peer, Peers.Event.ADD_INBOUND);
            }
            peer.setLastInboundRequest(Conch.getEpochTime());
            if (peerRequestHandler.rejectWhileDownloading()) {
                if (blockchainProcessor.isDownloading()) {
                    return DOWNLOADING;
                }
                if (Constants.isLightClient) {
                    return LIGHT_CLIENT;
                }
            }
            return peerRequestHandler.processRequest(request, peer);
        } catch (RuntimeException|ParseException|IOException e) {
            Logger.logDebugMessage("Error processing POST request: " + e.toString());
            peer.blacklist(e);
            return error(e);
        }
    }

    /**
     * WebSocket creator for peer connections
     */
    private class PeerSocketCreator implements WebSocketCreator  {
        /**
         * Create a peer WebSocket
         *
         * @param   req             WebSocket upgrade request
         * @param   resp            WebSocket upgrade response
         * @return                  WebSocket
         */
        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            return Peers.useWebSockets ? new PeerWebSocket(PeerServlet.this) : null;
        }
    }
}
