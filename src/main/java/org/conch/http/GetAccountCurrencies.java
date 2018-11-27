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
import org.conch.common.ConchException;
import org.conch.db.*;
import org.conch.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountCurrencies extends APIServlet.APIRequestHandler {

    static final GetAccountCurrencies instance = new GetAccountCurrencies();

    private GetAccountCurrencies() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MS}, "account", "currency", "height", "includeCurrencyInfo");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long accountId = ParameterParser.getAccountId(req, true);
        int height = ParameterParser.getHeight(req);
        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        if (currencyId == 0) {
            JSONObject response = new JSONObject();
            try (DbIterator<Account.AccountCurrency> accountCurrencies = Account.getAccountCurrencies(accountId, height, 0, -1)) {
                JSONArray currencyJSON = new JSONArray();
                while (accountCurrencies.hasNext()) {
                    currencyJSON.add(JSONData.accountCurrency(accountCurrencies.next(), false, includeCurrencyInfo));
                }
                response.put("accountCurrencies", currencyJSON);
                return response;
            }
        } else {
            Account.AccountCurrency accountCurrency = Account.getAccountCurrency(accountId, currencyId, height);
            if (accountCurrency != null) {
                return JSONData.accountCurrency(accountCurrency, false, includeCurrencyInfo);
            }
            return JSON.emptyJSON;
        }
    }

}
