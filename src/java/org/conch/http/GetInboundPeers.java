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

package org.conch.http;

import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.peer.Peers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>The GetInboundPeers API will return a list of inbound peers.
 * An inbound peer is a peer that has sent a request to this peer
 * within the previous 30 minutes.</p>
 *
 * <p>Request parameters:</p>
 * <ul>
 * <li>includePeerInfo - Specify 'true' to include the peer information
 * or 'false' to include just the peer address.  The default is 'false'.</li>
 * </ul>
 *
 * <p>Response parameters:</p>
 * <ul>
 * <li>peers - An array of peers</li>
 * </ul>
 *
 * <p>Error Response parameters:</p>
 * <ul>
 * <li>errorCode - API error code</li>
 * <li>errorDescription - API error description</li>
 * </ul>
 */
public final class GetInboundPeers extends APIServlet.APIRequestHandler {

    /** GetInboundPeers instance */
    static final GetInboundPeers instance = new GetInboundPeers();

    /**
     * Create the GetInboundPeers instance
     */
    private GetInboundPeers() {
        super(new APITag[] {APITag.NETWORK}, "includePeerInfo");
    }

    /**
     * Process the GetInboundPeers API request
     *
     * @param   req                 API request
     * @return                      API response or null
     */
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        boolean includePeerInfo = "true".equalsIgnoreCase(req.getParameter("includePeerInfo"));
        List<Peer> peers = Peers.getInboundPeers();
        JSONArray peersJSON = new JSONArray();
        if (includePeerInfo) {
            peers.forEach(peer -> peersJSON.add(JSONData.peer(peer)));
        } else {
            peers.forEach(peer -> peersJSON.add(peer.getHost()));
        }
        JSONObject response = new JSONObject();
        response.put("peers", peersJSON);
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
