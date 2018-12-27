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

import org.conch.asset.token.CurrencyFounder;
import org.conch.common.ConchException;
import org.conch.db.*;
import org.conch.db.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyFounders extends APIServlet.APIRequestHandler {

    static final GetCurrencyFounders instance = new GetCurrencyFounders();

    private GetCurrencyFounders() {
        super(new APITag[] {APITag.MS}, "currency", "account", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (currencyId == 0 && accountId == 0) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray foundersJSONArray = new JSONArray();
        response.put("founders", foundersJSONArray);

        if (currencyId != 0 && accountId != 0) {
            CurrencyFounder currencyFounder = CurrencyFounder.getFounder(currencyId, accountId);
            if (currencyFounder != null) {
                foundersJSONArray.add(JSONData.currencyFounder(currencyFounder));
            }
            return response;
        }

        DbIterator<CurrencyFounder> founders = null;
        try {
            if (accountId == 0) {
                founders = CurrencyFounder.getCurrencyFounders(currencyId, firstIndex, lastIndex);
            } else if (currencyId == 0) {
                founders = CurrencyFounder.getFounderCurrencies(accountId, firstIndex, lastIndex);
            }
            for (CurrencyFounder founder : founders) {
                foundersJSONArray.add(JSONData.currencyFounder(founder));
            }
        } finally {
            DbUtils.close(founders);
        }
        return response;
    }
}
