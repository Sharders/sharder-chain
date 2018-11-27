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

import org.conch.ConchException;
import org.conch.account.Account;
import org.conch.asset.token.Currency;
import org.conch.tx.Attachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Generate new currency units
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id of the minted currency</li>
 * <li>nonce - a unique nonce provided by the miner</li>
 * <li>units - number of units minted per this transaction</li>
 * <li>counter - a sequential number counting the ordinal mint operation number per currency/account combination<br>
 * this ever increasing value ensures that the same mint transaction cannot execute more than once</li>
 * </ul>
 *
 * Each minting request triggers a hash calculation based on the submitted data and the currency hash algorithm<br>
 * The resulting hash code is compared to the target value derived from the current difficulty.<br>
 * If the hash code is smaller than the target the currency units are generated into the sender account.<br>
 * It is recommended to calculate the hash value offline before submitting the transaction.<br>
 * Use the {@link GetMintingTarget} transaction to retrieve the current hash target and then calculate the hash offline
 * by following the procedure used in {@link org.conch.mint.CurrencyMint#mintCurrency}<br>
 */
public final class CurrencyMint extends CreateTransaction {

    static final CurrencyMint instance = new CurrencyMint();

    private CurrencyMint() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "nonce", "units", "counter");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Currency currency = ParameterParser.getCurrency(req);
        long nonce = ParameterParser.getLong(req, "nonce", Long.MIN_VALUE, Long.MAX_VALUE, true);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        long counter = ParameterParser.getLong(req, "counter", 0, Integer.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MonetarySystemCurrencyMinting(nonce, currency.getId(), units, counter);
        return createTransaction(req, account, attachment);
    }

}
