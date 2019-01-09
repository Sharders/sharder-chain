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
import org.conch.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Get a funding monitor
 * <p>
 * The monitors for a single funding account will be returned when the secret phrase is specified.
 * A single monitor will be returned if holding and property are specified.
 * Otherwise, all monitors for the funding account will be returned
 * The administrator password is not required and will be ignored.
 * <p>
 * When the administrator password is specified, all monitors will be returned
 * unless the funding account is also specified.  A single monitor will be returned if
 * holding and property are specified.  Otherwise, all monitors for the
 * funding account will be returned.
 * <p>
 * Holding type codes are listed in getConstants.
 * In addition, the holding identifier must be specified when the holding type is ASSET or CURRENCY.
 */
public class GetFundingMonitor extends APIServlet.APIRequestHandler {

    static final GetFundingMonitor instance = new GetFundingMonitor();

    private GetFundingMonitor() {
        super(new APITag[] {APITag.ACCOUNTS}, "holdingType", "holding", "property", "secretPhrase",
                "includeMonitoredAccounts", "account", "adminPassword");
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
        long account = ParameterParser.getAccountId(req, false);
        boolean includeMonitoredAccounts = "true".equalsIgnoreCase(req.getParameter("includeMonitoredAccounts"));
        if (secretPhrase == null) {
            API.verifyPassword(req);
        }
        List<FundingMonitor> monitors;
        if (secretPhrase != null || account != 0) {
            if (secretPhrase != null) {
                if (account != 0) {
                    if (Account.getId(Crypto.getPublicKey(secretPhrase)) != account) {
                        return JSONResponses.INCORRECT_ACCOUNT;
                    }
                } else {
                    account = Account.getId(Crypto.getPublicKey(secretPhrase));
                }
            }
            final long accountId = account;
            final HoldingType holdingType = ParameterParser.getHoldingType(req);
            final long holdingId = ParameterParser.getHoldingId(req, holdingType);
            final String property = ParameterParser.getAccountProperty(req, false);
            Filter<FundingMonitor> filter;
            if (property != null) {
                filter = (monitor) -> monitor.getAccountId() == accountId &&
                        monitor.getProperty().equals(property) &&
                        monitor.getHoldingType() == holdingType &&
                        monitor.getHoldingId() == holdingId;
            } else {
                filter = (monitor) -> monitor.getAccountId() == accountId;
            }
            monitors = FundingMonitor.getMonitors(filter);
        } else {
            monitors = FundingMonitor.getAllMonitors();
        }
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        monitors.forEach(monitor -> {
            JSONObject monitorJSON = JSONData.accountMonitor(monitor, includeMonitoredAccounts);
            jsonArray.add(monitorJSON);
        });
        response.put("monitors", jsonArray);
        return response;
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
