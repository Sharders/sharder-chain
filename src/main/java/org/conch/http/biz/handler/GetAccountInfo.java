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

package org.conch.http.biz.handler;

import org.conch.Account;
import org.conch.ConchException;
import org.conch.http.APIServlet;
import org.conch.http.APITag;
import org.conch.http.JSONData;
import org.conch.http.ParameterParser;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

public final class GetAccountInfo extends APIServlet.APIRequestHandler {

    public static final GetAccountInfo instance = new GetAccountInfo();

    private GetAccountInfo() {
        super(new APITag[] {APITag.BIZ}, "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        Account account = ParameterParser.getAccount(req);
        JSONObject response = JSONData.accountBalance(account, false);
        JSONData.putAccount(response, "account", account.getId());

        byte[] publicKey = Account.getPublicKey(account.getId());
        if (publicKey != null) {
            response.put("publicKey", Convert.toHexString(publicKey));
        }
        Account.AccountInfo accountInfo = account.getAccountInfo();
        if (accountInfo != null) {
            response.put("name", Convert.nullToEmpty(accountInfo.getName()));
            response.put("description", Convert.nullToEmpty(accountInfo.getDescription()));
        }
        response.put("balance", BigDecimal.valueOf(account.getBalanceNQT()).divide(BigDecimal.valueOf(100000000L)));
        response.put("forgedBalance", BigDecimal.valueOf(account.getForgedBalanceNQT()).divide(BigDecimal.valueOf(100000000L)));
        response.put("frozenBalance", BigDecimal.valueOf(account.getFrozenBalanceNQT()).divide(BigDecimal.valueOf(100000000L)));
        response.remove("unconfirmedBalanceNQT");
        response.remove("forgedBalanceNQT");
        response.remove("balanceNQT");
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
