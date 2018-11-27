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

import org.conch.asset.AssetDelete;
import org.conch.common.ConchException;
import org.conch.db.*;
import org.conch.db.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetDeletes extends APIServlet.APIRequestHandler {

    static final GetAssetDeletes instance = new GetAssetDeletes();

    private GetAssetDeletes() {
        super(new APITag[] {APITag.AE}, "asset", "account", "firstIndex", "lastIndex", "timestamp", "includeAssetInfo");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (assetId == 0 && accountId == 0) {
            return JSONResponses.MISSING_ASSET_ACCOUNT;
        }
        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray deletesData = new JSONArray();
        DbIterator<AssetDelete> deletes = null;
        try {
            if (accountId == 0) {
                deletes = AssetDelete.getAssetDeletes(assetId, firstIndex, lastIndex);
            } else if (assetId == 0) {
                deletes = AssetDelete.getAccountAssetDeletes(accountId, firstIndex, lastIndex);
            } else {
                deletes = AssetDelete.getAccountAssetDeletes(accountId, assetId, firstIndex, lastIndex);
            }
            while (deletes.hasNext()) {
                AssetDelete assetDelete = deletes.next();
                if (assetDelete.getTimestamp() < timestamp) {
                    break;
                }
                deletesData.add(JSONData.assetDelete(assetDelete, includeAssetInfo));
            }
        } finally {
            DbUtils.close(deletes);
        }
        response.put("deletes", deletesData);

        return response;
    }

    @Override
    protected boolean startDbTransaction() {
        return true;
    }
}
