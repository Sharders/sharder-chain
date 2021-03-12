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
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.market.DigitalGoodsStore;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSExpiredPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSExpiredPurchases instance = new GetDGSExpiredPurchases();

    private GetDGSExpiredPurchases() {
        super(new APITag[] {APITag.DGS}, "seller", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long sellerId = ParameterParser.getAccountId(req, "seller", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        
        DbIterator<DigitalGoodsStore.Purchase> purchases = null;
        try {
            purchases = DigitalGoodsStore.Purchase.getExpiredSellerPurchases(sellerId, firstIndex, lastIndex);
            while (purchases.hasNext()) {
                purchasesJSON.add(JSONData.purchase(purchases.next()));
            }
        }finally {
            DbUtils.close(purchases);
        }

        response.put("purchases", purchasesJSON);
        return response;
    }

}
