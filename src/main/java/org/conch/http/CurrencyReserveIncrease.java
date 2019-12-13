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
import org.conch.asset.token.Currency;
import org.conch.asset.token.CurrencyType;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.tx.Attachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Increase the value of currency units by paying MWFS
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>amountPerUnitNQT - the NQT amount invested into increasing the value of a single currency unit.<br>
 * This value is multiplied by the currency total supply and the result is deducted from the sender's account balance.
 * </ul>
 * <p>
 * Constraints
 * <p>
 * This API is allowed only when the currency is {@link CurrencyType#RESERVABLE} and is not yet active.
 * <p>
 * The sender account is registered as a founder. Once the currency becomes active
 * the total supply is distributed between the founders based on their proportional investment<br>
 * The list of founders and their NQT investment can be obtained using the {@link GetCurrencyFounders} API.
 */

public final class CurrencyReserveIncrease extends CreateTransaction {

    static final CurrencyReserveIncrease instance = new CurrencyReserveIncrease();

    private CurrencyReserveIncrease() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "amountPerUnitNQT");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Currency currency = ParameterParser.getCurrency(req);
        long amountPerUnitNQT = ParameterParser.getLong(req, "amountPerUnitNQT", 1L, Constants.MAX_BALANCE_NQT, true);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemReserveIncrease(currency.getId(), amountPerUnitNQT);
        return createTransaction(req, account, attachment);

    }

}
