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
import org.conch.util.Convert;
import org.conch.vote.Poll;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public final class SearchPolls extends APIServlet.APIRequestHandler {

    static final SearchPolls instance = new SearchPolls();

    private SearchPolls() {
        super(new APITag[] {APITag.VS, APITag.SEARCH}, "query", "firstIndex", "lastIndex", "includeFinished");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        if (query.isEmpty()) {
            return JSONResponses.missing("query");
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try (DbIterator<Poll> polls = Poll.searchPolls(query, includeFinished, firstIndex, lastIndex)) {
            while (polls.hasNext()) {
                jsonArray.add(JSONData.poll(polls.next()));
            }
        }
        response.put("polls", jsonArray);
        return response;
    }

}
