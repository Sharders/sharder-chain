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

import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetAccounts extends APIServlet.APIRequestHandler {

    static final GetAssetAccounts instance = new GetAssetAccounts();

    private GetAssetAccounts() {
        super(new APITag[] {APITag.AE}, "asset", "height", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int height = ParameterParser.getHeight(req);

        JSONArray accountAssets = new JSONArray();

        DbIterator<Account.AccountAsset> iterator = null;
        try {
            iterator = Account.getAssetAccounts(assetId, height, firstIndex, lastIndex);
            while (iterator.hasNext()) {
                Account.AccountAsset accountAsset = iterator.next();
                accountAssets.add(JSONData.accountAsset(accountAsset, true, false));
            }
        }finally {
            DbUtils.close(iterator);
        }

        JSONObject response = new JSONObject();
        response.put("accountAssets", accountAssets);
        return response;

    }

}
