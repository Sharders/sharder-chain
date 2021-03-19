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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetPeers extends PeerServlet.PeerRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray services = new JSONArray();
        // compatible
        boolean notFilter = Boolean.getBoolean((String) request.get("notFilter"));
        if (notFilter) {
            Peers.getAllPeers().forEach(otherPeer -> {
                if (otherPeer.getAnnouncedAddress() != null && otherPeer.shareAddress()) {
                    jsonArray.add(otherPeer.getAnnouncedAddress());
                    services.add(Long.toUnsignedString(((PeerImpl)otherPeer).getServices()));
                }
            });
        } else {
            Peers.getAllPeers().forEach(otherPeer -> {
                if (!otherPeer.isBlacklisted() && otherPeer.getAnnouncedAddress() != null
                        && otherPeer.getState() == Peer.State.CONNECTED && otherPeer.shareAddress()) {
                    jsonArray.add(otherPeer.getAnnouncedAddress());
                    services.add(Long.toUnsignedString(((PeerImpl)otherPeer).getServices()));
                }
            });
        }
        response.put("peers", jsonArray);
        response.put("services", services);         // Separate array for backwards compatibility
        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return false;
    }

}
