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

import org.conch.Exchange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetLastExchanges extends APIServlet.APIRequestHandler {

    static final GetLastExchanges instance = new GetLastExchanges();

    private GetLastExchanges() {
        super(new APITag[] {APITag.MS}, "currencies", "currencies", "currencies"); // limit to 3 for testing
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] currencyIds = ParameterParser.getUnsignedLongs(req, "currencies");
        JSONArray exchangesJSON = new JSONArray();
        List<Exchange> exchanges = Exchange.getLastExchanges(currencyIds);
        exchanges.forEach(exchange -> exchangesJSON.add(JSONData.exchange(exchange, false)));
        JSONObject response = new JSONObject();
        response.put("exchanges", exchangesJSON);
        return response;
    }

}
