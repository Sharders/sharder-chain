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

import org.conch.common.ConchException;
import org.conch.db.*;
import org.conch.market.Order;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAskOrderIds extends APIServlet.APIRequestHandler {

    static final GetAskOrderIds instance = new GetAskOrderIds();

    private GetAskOrderIds() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray orderIds = new JSONArray();
        DbIterator<Order.Ask> askOrders = null;
        try {
            askOrders = Order.Ask.getSortedOrders(assetId, firstIndex, lastIndex);
            while (askOrders.hasNext()) {
                orderIds.add(Long.toUnsignedString(askOrders.next().getId()));
            }
        }finally {
            DbUtils.close(askOrders);
        }

        JSONObject response = new JSONObject();
        response.put("askOrderIds", orderIds);
        return response;

    }

}
