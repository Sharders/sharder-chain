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

import org.conch.account.Account;
import org.conch.db.*;
import org.conch.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchAccounts extends APIServlet.APIRequestHandler {

    static final SearchAccounts instance = new SearchAccounts();

    private SearchAccounts() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.SEARCH}, "query", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        if (query.isEmpty()) {
            return JSONResponses.missing("query");
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        try (DbIterator<Account.AccountInfo> accounts = Account.searchAccounts(query, firstIndex, lastIndex)) {
            for (Account.AccountInfo account : accounts) {
                JSONObject accountJSON = new JSONObject();
                JSONData.putAccount(accountJSON, "account", account.getAccountId());
                if (account.getName() != null) {
                    accountJSON.put("name", account.getName());
                }
                if (account.getDescription() != null) {
                    accountJSON.put("description", account.getDescription());
                }
                accountsJSONArray.add(accountJSON);
            }
        }
        response.put("accounts", accountsJSONArray);
        return response;
    }

}
