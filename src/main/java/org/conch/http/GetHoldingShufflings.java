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

import org.conch.db.*;
import org.conch.shuffle.Shuffling;
import org.conch.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.incorrect;

public final class GetHoldingShufflings extends APIServlet.APIRequestHandler {

    static final GetHoldingShufflings instance = new GetHoldingShufflings();

    private GetHoldingShufflings() {
        super(new APITag[] {APITag.SHUFFLING}, "holding", "stage", "includeFinished", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long holdingId = 0;
        String holdingValue = Convert.emptyToNull(req.getParameter("holding"));
        if (holdingValue != null) {
            try {
                holdingId = Convert.parseUnsignedLong(holdingValue);
            } catch (RuntimeException e) {
                return incorrect("holding");
            }
        }
        String stageValue = Convert.emptyToNull(req.getParameter("stage"));
        Shuffling.Stage stage = null;
        if (stageValue != null) {
            try {
                stage = Shuffling.Stage.get(Byte.parseByte(stageValue));
            } catch (RuntimeException e) {
                return incorrect("stage");
            }
        }
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("shufflings", jsonArray);

        DbIterator<Shuffling> shufflings = null;
        try {
            shufflings = Shuffling.getHoldingShufflings(holdingId, stage, includeFinished, firstIndex, lastIndex);
            for (Shuffling shuffling : shufflings) {
                jsonArray.add(JSONData.shuffling(shuffling, false));
            }
        }finally {
            DbUtils.close(shufflings);
        }
        return response;
    }

}
