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

import org.conch.Conch;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;
import java.util.Map;

final class GetInfo extends PeerServlet.PeerRequestHandler {

    static final GetInfo instance = new GetInfo();

    private static final JSONStreamAware INVALID_ANNOUNCED_ADDRESS;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.INVALID_ANNOUNCED_ADDRESS);
        INVALID_ANNOUNCED_ADDRESS = JSON.prepare(response);
    }

    private GetInfo() {}

    /**
     * validate the connected peer info.
     * update the peer info of my peer list after validation passed.
     *  return my peer info finally.
     * @param request
     * @param peer
     * @return
     */
    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        peerImpl.setLastUpdated(Conch.getEpochTime());
        long origServices = peerImpl.getServices();
        String servicesString = (String)request.get("services");
        peerImpl.setServices(servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);
        peerImpl.analyzeHallmark((String)request.get("hallmark"));
        boolean isDesktopMode = Peer.RunningMode.DESKTOP.matchName((String)request.get("runningMode"));
        boolean useNATService = (Boolean)request.get("useNATService");
        boolean normalNodeWithoutNat = isDesktopMode && useNATService;
        if(normalNodeWithoutNat) Logger.logDebugMessage("GetInfo: peer is normal node without the NAT service, don't check the announced address and reject it");
        if (!Peers.ignorePeerAnnouncedAddress) {
            String announcedAddress = Convert.emptyToNull((String) request.get("announcedAddress"));

            // check the announced address
            if (announcedAddress != null) {
                announcedAddress = Peers.addressWithPort(announcedAddress.toLowerCase());
                // don't check the normal node without nat service, because the normal node's announced address is the intranet ip
                if (normalNodeWithoutNat && announcedAddress != null) {
                    // check the announced address whether match the ip
                    if (!peerImpl.verifyAnnouncedAddress(announcedAddress)) {
                        Logger.logDebugMessage("GetInfo: ignoring invalid announced address for " + peerImpl.getHost());
                        if (!peerImpl.verifyAnnouncedAddress(peerImpl.getAnnouncedAddress())) {
                            Logger.logDebugMessage("GetInfo: old announced address for " + peerImpl.getHost() + " no longer valid");
                            Peers.setAnnouncedAddress(peerImpl, null);
                        }
                        peerImpl.setState(Peer.State.NON_CONNECTED);
                        return INVALID_ANNOUNCED_ADDRESS;
                    }
                    // update the connected status according to announced address
                    if (!announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                        Logger.logDebugMessage("GetInfo: peer " + peer.getHost() + " changed announced address from " + peer.getAnnouncedAddress() + " to " + announcedAddress);
                        int oldPort = peerImpl.getPort();
                        Peers.setAnnouncedAddress(peerImpl, announcedAddress);
                        if (peerImpl.getPort() != oldPort) {
                            // force checking connectivity to new announced port
                            peerImpl.setState(Peer.State.NON_CONNECTED);
                        }
                    }
                } else {
                    // update the announced address of peer in my local peer list
                    Peers.setAnnouncedAddress(peerImpl, null);
                }
            }
        }

        // update and notify peer info changed
        String application = (String)request.get("application");
        if (application == null) {
            application = "?";
        }
        peerImpl.setApplication(application.trim());

        String version = (String)request.get("version");
        if (version == null) {
            version = "?";
        }
        peerImpl.setVersion(version.trim());

        String platform = (String)request.get("platform");
        if (platform == null) {
            platform = "?";
        }
        peerImpl.setPlatform(platform.trim());
        peerImpl.setShareAddress(Boolean.TRUE.equals(request.get("shareAddress")));
        peerImpl.setApiPort(request.get("apiPort"));
        peerImpl.setApiSSLPort(request.get("apiSSLPort"));
        peerImpl.setDisabledAPIs(request.get("disabledAPIs"));
        peerImpl.setApiServerIdleTimeout(request.get("apiServerIdleTimeout"));
        peerImpl.setBlockchainState(request.get("blockchainState"));

        if (peerImpl.getServices() != origServices) {
            Peers.notifyListeners(peerImpl, Peers.Event.CHANGED_SERVICES);
        }
        // CollectForkNode save or update forkBlocks,make sure blocks is up to date
        if (request.get("forkBlocks") != null && Peers.isCollectForkNode(Conch.getMyAddress())) {
            List<JSONObject> forkBlocks = (List<JSONObject>) request.get("forkBlocks");
            Logger.logDebugMessage("SaveOrUpdate forkBlocks of commonNode[%s] and BindRSAccount[%s]", peerImpl.getAnnouncedAddress(), peerImpl.getBindRsAccount());
            Peers.saveOrUpdateForkBlocks(peerImpl.getBindRsAccount(), forkBlocks);
            // TEST-Start return missedForkBlocks to commonNode
            /*JSONObject jsonObject = new JSONObject();
            jsonObject.put("startHeight", Conch.getHeight()-Peers.forkBlocksLevel.MINI.getLevel());
            jsonObject.put("endHeight", Conch.getHeight());
            Peers.missingForkBlocksMap.put(peerImpl.getBindRsAccount(), jsonObject);*/
            // TEST-End
            if (Peers.missingForkBlocksMap.get(peerImpl.getBindRsAccount()) != null && request.get("processForkNode") == null) {
                Logger.logDebugMessage("Report missedBlocks to commonNode[%s]", peerImpl.getAnnouncedAddress());
                return Peers.getMyPeerInfoResponseToCommonNode(peerImpl.getBindRsAccount());
            }
        }
        // ForkData is sent to processForkNode when it calls the API
        if (request.get("processForkNode") != null
                && (boolean) request.get("processForkNode") == true
                && Peers.isCollectForkNode(Conch.getMyAddress())) {
            if (request.get("missedBlocksMap") != null) {
                Map<String, JSONObject> missedBlocksMap = (Map<String, JSONObject>) request.get("missedBlocksMap");
                if (!missedBlocksMap.isEmpty()) {
                    Peers.missingForkBlocksMap.putAll(missedBlocksMap);
                }
            }
            Logger.logDebugMessage("Report and generate own forkBlocks to processForkNode[%s] and BindRSAccount[%s]", peerImpl.getAnnouncedAddress(), peerImpl.getBindRsAccount());
            return Peers.getMyPeerInfoResponseToProcessForkNode();
        }
        return Peers.getMyPeerInfoResponse();

    }

    @Override
    boolean rejectWhileDownloading() {
        return false;
    }

}
