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
import org.conch.common.Constants;
import org.conch.tx.Attachment;
import org.conch.tx.PhasingParams;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
/**
 * Sets an account control that blocks transactions unless they are phased with certain parameters
 * 
 * <p>
 * Parameters
 * <ul>
 * <li>controlVotingModel - The expected voting model of the phasing. Possible values: 
 *  <ul>
 *  <li>NONE(-1) - the phasing control is removed</li>
 *  <li>ACCOUNT(0) - only by-account voting is allowed</li>
 *  <li>NQT(1) - only balance voting is allowed</li>
 *  <li>ASSET(2) - only asset voting is allowed</li>
 *  <li>CURRENCY(3) - only currency voting is allowed</li>
 *  </ul>
 * </li>
 * <li>controlQuorum - The expected quorum.</li>
 * <li>controlMinBalance - The expected minimum balance</li>
 * <li>controlMinBalanceModel - The expected minimum balance model. Possible values:
 * <ul>
 *  <li>NONE(0) No minimum balance restriction</li>
 *  <li>NQT(1) CDWH balance threshold</li>
 *  <li>ASSET(2) Asset balance threshold</li>
 *  <li>CURRENCY(3) Currency balance threshold</li>
 * </ul>
 * </li>
 * <li>controlHolding - The expected holding ID - asset ID or currency ID.</li>
 * <li>controlWhitelisted - multiple values - the expected whitelisted accounts</li>
 * <li>controlMaxFees - The maximum allowed accumulated total fees for not yet finished phased transactions.</li>
 * <li>controlMinDuration - The minimum phasing duration (finish height minus current height).</li>
 * <li>controlHolding - The maximum allowed phasing duration.</li>
 * </ul>
 *
 * 
 */
public final class SetPhasingOnlyControl extends CreateTransaction {

    static final SetPhasingOnlyControl instance = new SetPhasingOnlyControl();

    private SetPhasingOnlyControl() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL, APITag.CREATE_TRANSACTION}, "controlVotingModel", "controlQuorum", "controlMinBalance",
                "controlMinBalanceModel", "controlHolding", "controlWhitelisted", "controlWhitelisted", "controlWhitelisted",
                "controlMaxFees", "controlMinDuration", "controlMaxDuration");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        Account account = ParameterParser.getSenderAccount(request);
        PhasingParams phasingParams = parsePhasingParams(request, "control");
        long maxFees = ParameterParser.getLong(request, "controlMaxFees", 0, Constants.MAX_BALANCE_NQT, false);
        short minDuration = (short)ParameterParser.getInt(request, "controlMinDuration", 0, Constants.MAX_PHASING_DURATION - 1, false);
        short maxDuration = (short) ParameterParser.getInt(request, "controlMaxDuration", 0, Constants.MAX_PHASING_DURATION - 1, false);
        return createTransaction(request, account, new Attachment.SetPhasingOnly(phasingParams, maxFees, minDuration, maxDuration));
    }

}
