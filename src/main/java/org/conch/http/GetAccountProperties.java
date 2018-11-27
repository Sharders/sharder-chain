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

import org.conch.ConchException;
import org.conch.account.Account;
import org.conch.db.DbIterator;
import org.conch.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountProperties extends APIServlet.APIRequestHandler {

    static final GetAccountProperties instance = new GetAccountProperties();

    private GetAccountProperties() {
        super(new APITag[] {APITag.ACCOUNTS}, "recipient", "property", "setter", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        long setterId = ParameterParser.getAccountId(req, "setter", false);
        if (recipientId == 0 && setterId == 0) {
            return JSONResponses.missing("recipient", "setter");
        }
        String property = Convert.emptyToNull(req.getParameter("property"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray propertiesJSON = new JSONArray();
        response.put("properties", propertiesJSON);
        if (recipientId != 0) {
            JSONData.putAccount(response, "recipient", recipientId);
        }
        if (setterId != 0) {
            JSONData.putAccount(response, "setter", setterId);
        }
        try (DbIterator<Account.AccountProperty> iterator = Account.getProperties(recipientId, setterId, property, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                propertiesJSON.add(JSONData.accountProperty(iterator.next(), recipientId == 0, setterId == 0));
            }
        }
        return response;

    }

}
