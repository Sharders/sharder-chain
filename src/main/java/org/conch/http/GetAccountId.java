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
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GetAccountId extends APIServlet.APIRequestHandler {

    static final GetAccountId instance = new GetAccountId();

    private GetAccountId() {
        super(new APITag[] {APITag.ACCOUNTS}, "secretPhrase", "publicKey");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String accountIds = req.getParameter("accountId");
        JSONObject response = new JSONObject();
        Map<String, String> rsAccountMap = new HashMap<String, String>();
        List<Map> lm = new ArrayList<Map>();
        if(accountIds!=null){
            if(accountIds.contains(",")){
                String[] accountIdArr = accountIds.split(",");
                for (int i = 0; i < accountIdArr.length; i++){
                    rsAccountMap.putIfAbsent("accountId",accountIdArr[i]);
                    rsAccountMap.putIfAbsent("rsaccountId",Account.rsAccount(Long.parseLong(accountIdArr[i])));
                    lm.add(rsAccountMap);
                }
            }else {
                rsAccountMap.put("accountId",accountIds);
                rsAccountMap.put("rsaccountId",Account.rsAccount(Long.parseLong(accountIds)));
                lm.add(rsAccountMap);
            }
            response.put("rsAccountInfo", lm);
        }
        else {
            byte[] publicKey = ParameterParser.getPublicKey(req);
            long accountId = Account.getId(publicKey);

            JSONData.putAccount(response, "account", accountId);
            response.put("publicKey", Convert.toHexString(publicKey));
        }

        return response;
    }

    @Override
    protected final boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected final boolean requireBlockchain() {
        return false;
    }

}
