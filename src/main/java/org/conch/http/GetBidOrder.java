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
import org.conch.market.Order;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBidOrder extends APIServlet.APIRequestHandler {

    static final GetBidOrder instance = new GetBidOrder();

    private GetBidOrder() {
        super(new APITag[] {APITag.AE}, "order");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        long orderId = ParameterParser.getUnsignedLong(req, "order", true);
        Order.Bid bidOrder = Order.Bid.getBidOrder(orderId);
        if (bidOrder == null) {
            return JSONResponses.UNKNOWN_ORDER;
        }
        return JSONData.bidOrder(bidOrder);
    }

}
