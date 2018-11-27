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

import org.conch.account.AccountRestrictions;
import org.conch.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Returns the phasing control certain account. The result contains the following entries similar to the control* parameters of {@link SetPhasingOnlyControl}
 * 
 * <ul>
 * <li>votingModel - See {@link SetPhasingOnlyControl} for possible values. NONE(-1) means not control is set</li>
 * <li>quorum</li>
 * <li>minBalance</li>
 * <li>minBalanceModel - See {@link SetPhasingOnlyControl} for possible values</li>
 * <li>holding</li>
 * <li>whitelisted - array of whitelisted voter account IDs</li>
 * </ul>
 * 
 * <p>
 * Parameters
 * <ul>
 * <li>account - the account for which the phasing control is queried</li>
 * </ul>
 * 
 * 
 * @see SetPhasingOnlyControl
 * 
 */
public final class GetPhasingOnlyControl extends APIServlet.APIRequestHandler {

    static final GetPhasingOnlyControl instance = new GetPhasingOnlyControl();
    
    private GetPhasingOnlyControl() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL}, "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long accountId = ParameterParser.getAccountId(req, true);
        AccountRestrictions.PhasingOnly phasingOnly = AccountRestrictions.PhasingOnly.get(accountId);
        return phasingOnly == null ? JSON.emptyJSON : JSONData.phasingOnly(phasingOnly);
    }

}
