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

import org.conch.asset.token.CurrencyTransfer;
import org.conch.common.ConchException;
import org.conch.db.*;
import org.conch.db.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyTransfers extends APIServlet.APIRequestHandler {

    static final GetCurrencyTransfers instance = new GetCurrencyTransfers();

    private GetCurrencyTransfers() {
        super(new APITag[] {APITag.MS}, "currency", "account", "firstIndex", "lastIndex", "timestamp", "includeCurrencyInfo");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (currencyId == 0 && accountId == 0) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));
        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray transfersData = new JSONArray();
        DbIterator<CurrencyTransfer> transfers = null;
        try {
            if (accountId == 0) {
                transfers = CurrencyTransfer.getCurrencyTransfers(currencyId, firstIndex, lastIndex);
            } else if (currencyId == 0) {
                transfers = CurrencyTransfer.getAccountCurrencyTransfers(accountId, firstIndex, lastIndex);
            } else {
                transfers = CurrencyTransfer.getAccountCurrencyTransfers(accountId, currencyId, firstIndex, lastIndex);
            }
            while (transfers.hasNext()) {
                CurrencyTransfer currencyTransfer = transfers.next();
                if (currencyTransfer.getTimestamp() < timestamp) {
                    break;
                }
                transfersData.add(JSONData.currencyTransfer(currencyTransfer, includeCurrencyInfo));
            }
        } finally {
            DbUtils.close(transfers);
        }
        response.put("transfers", transfersData);

        return response;
    }

    @Override
    protected boolean startDbTransaction() {
        return true;
    }

}
