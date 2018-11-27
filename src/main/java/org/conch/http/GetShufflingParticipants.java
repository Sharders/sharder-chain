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

import org.conch.common.ConchException;
import org.conch.db.DbIterator;
import org.conch.shuffle.ShufflingParticipant;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetShufflingParticipants extends APIServlet.APIRequestHandler {

    static final GetShufflingParticipants instance = new GetShufflingParticipants();

    private GetShufflingParticipants() {
        super(new APITag[] {APITag.SHUFFLING}, "shuffling");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        long shufflingId = ParameterParser.getUnsignedLong(req, "shuffling", true);
        JSONObject response = new JSONObject();
        JSONArray participantsJSONArray = new JSONArray();
        response.put("participants", participantsJSONArray);
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(shufflingId)) {
            for (ShufflingParticipant participant : participants) {
                participantsJSONArray.add(JSONData.participant(participant));
            }
        }
        return response;
    }

}
