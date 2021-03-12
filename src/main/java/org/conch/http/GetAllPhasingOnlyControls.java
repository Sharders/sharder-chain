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

import org.conch.account.AccountRestrictions;
import org.conch.account.AccountRestrictions.PhasingOnly;
import org.conch.db.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllPhasingOnlyControls extends APIServlet.APIRequestHandler {

    static final GetAllPhasingOnlyControls instance = new GetAllPhasingOnlyControls();

    private GetAllPhasingOnlyControls() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL}, "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        DbIterator<PhasingOnly> iterator = null;
        try {
            iterator = AccountRestrictions.PhasingOnly.getAll(firstIndex, lastIndex);
            for (AccountRestrictions.PhasingOnly phasingOnly : iterator) {
                jsonArray.add(JSONData.phasingOnly(phasingOnly));
            }
        }finally {
            DbUtils.close(iterator);
        }
        response.put("phasingOnlyControls", jsonArray);
        return response;
    }

}
