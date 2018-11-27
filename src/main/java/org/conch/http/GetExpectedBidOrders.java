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

import org.conch.Conch;
import org.conch.ConchException;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GetExpectedBidOrders extends APIServlet.APIRequestHandler {

    static final GetExpectedBidOrders instance = new GetExpectedBidOrders();

    private GetExpectedBidOrders() {
        super(new APITag[] {APITag.AE}, "asset", "sortByPrice");
    }

    private final Comparator<Transaction> priceComparator = (o1, o2) -> {
        Attachment.ColoredCoinsOrderPlacement a1 = (Attachment.ColoredCoinsOrderPlacement)o1.getAttachment();
        Attachment.ColoredCoinsOrderPlacement a2 = (Attachment.ColoredCoinsOrderPlacement)o2.getAttachment();
        return Long.compare(a2.getPriceNQT(), a1.getPriceNQT());
    };

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", false);
        boolean sortByPrice = "true".equalsIgnoreCase(req.getParameter("sortByPrice"));
        Filter<Transaction> filter = transaction -> {
            if (transaction.getType() != TransactionType.ColoredCoins.BID_ORDER_PLACEMENT) {
                return false;
            }
            Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.getAttachment();
            return assetId == 0 || attachment.getAssetId() == assetId;
        };

        List<? extends Transaction> transactions = Conch.getBlockchain().getExpectedTransactions(filter);
        if (sortByPrice) {
            Collections.sort(transactions, priceComparator);
        }
        JSONArray orders = new JSONArray();
        transactions.forEach(transaction -> orders.add(JSONData.expectedBidOrder(transaction)));
        JSONObject response = new JSONObject();
        response.put("bidOrders", orders);
        return response;

    }

}
