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
import org.conch.account.FundingMonitor;
import org.conch.asset.HoldingType;
import org.conch.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Stop a funding monitor
 * <p>
 * When the secret phrase is specified, a single monitor will be stopped.
 * The monitor is identified by the secret phrase, holding and account property.
 * The administrator password is not required and will be ignored.
 * <p>
 * When the administrator password is specified, a single monitor can be
 * stopped by specifying the funding account, holding and account property.
 * If no account is specified, all monitors will be stopped.
 * <p>
 * The holding type and account property name must be specified when the secret
 * phrase or account is specified. Holding type codes are listed in getConstants.
 * In addition, the holding identifier must be specified when the holding type is ASSET or CURRENCY.
 */
public class StopFundingMonitor extends APIServlet.APIRequestHandler {

    static final StopFundingMonitor instance = new StopFundingMonitor();

    private StopFundingMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "holdingType", "holding", "property", "secretPhrase",
                "account", "adminPassword");
    }
    /**
     * Process the request
     *
     * @param   req                 Client request
     * @return                      Client response
     * @throws  ParameterException        Unable to process request
     */
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        long accountId = ParameterParser.getAccountId(req, false);
        JSONObject response = new JSONObject();
        if (secretPhrase == null) {
            API.verifyPassword(req);
        }
        if (secretPhrase != null || accountId != 0) {
            if (secretPhrase != null) {
                if (accountId != 0) {
                    if (Account.getId(Crypto.getPublicKey(secretPhrase)) != accountId) {
                        return JSONResponses.INCORRECT_ACCOUNT;
                    }
                } else {
                    accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
                }
            }
            HoldingType holdingType = ParameterParser.getHoldingType(req);
            long holdingId = ParameterParser.getHoldingId(req, holdingType);
            String property = ParameterParser.getAccountProperty(req, true);
            boolean stopped = FundingMonitor.stopMonitor(holdingType, holdingId, property, accountId);
            response.put("stopped", stopped ? 1 : 0);
        } else {
            int count = FundingMonitor.stopAllMonitors();
            response.put("stopped", count);
        }
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

}
