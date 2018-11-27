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
import org.conch.db.DbIterator;
import org.conch.market.Exchange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllExchanges extends APIServlet.APIRequestHandler {

    static final GetAllExchanges instance = new GetAllExchanges();

    private GetAllExchanges() {
        super(new APITag[] {APITag.MS}, "timestamp", "firstIndex", "lastIndex", "includeCurrencyInfo");
    }
    
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        final int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        JSONObject response = new JSONObject();
        JSONArray exchanges = new JSONArray();
        try (DbIterator<Exchange> exchangeIterator = Exchange.getAllExchanges(firstIndex, lastIndex)) {
            while (exchangeIterator.hasNext()) {
                Exchange exchange = exchangeIterator.next();
                if (exchange.getTimestamp() < timestamp) {
                    break;
                }
                exchanges.add(JSONData.exchange(exchange, includeCurrencyInfo));
            }
        }
        response.put("exchanges", exchanges);
        return response;
    }

}
