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

import org.conch.Asset;
import org.conch.db.DbIterator;
import org.conch.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetsByIssuer extends APIServlet.APIRequestHandler {

    static final GetAssetsByIssuer instance = new GetAssetsByIssuer();

    private GetAssetsByIssuer() {
        super(new APITag[] {APITag.AE, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex", "includeCounts");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] accountIds = ParameterParser.getAccountIds(req, true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeCounts = "true".equalsIgnoreCase(req.getParameter("includeCounts"));

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        response.put("assets", accountsJSONArray);
        for (long accountId : accountIds) {
            JSONArray assetsJSONArray = new JSONArray();
            try (DbIterator<Asset> assets = Asset.getAssetsIssuedBy(accountId, firstIndex, lastIndex)) {
                while (assets.hasNext()) {
                    assetsJSONArray.add(JSONData.asset(assets.next(), includeCounts));
                }
            }
            accountsJSONArray.add(assetsJSONArray);
        }
        return response;
    }

}
