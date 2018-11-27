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
import org.conch.asset.token.Currency;
import org.conch.mint.CurrencyMint;
import org.conch.mint.CurrencyMinting;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

/**
 * Currency miners can use this API to obtain their target hash value for minting currency units
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>account - miner account id
 * <li>units - number of currency units the miner is trying to mint
 * </ul>
 */
public final class GetMintingTarget extends APIServlet.APIRequestHandler {

    static final GetMintingTarget instance = new GetMintingTarget();

    private GetMintingTarget() {
        super(new APITag[] {APITag.MS}, "currency", "account", "units");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Currency currency = ParameterParser.getCurrency(req);
        JSONObject json = new JSONObject();
        json.put("currency", Long.toUnsignedString(currency.getId()));
        long units = ParameterParser.getLong(req, "units", 1, currency.getMaxSupply() - currency.getReserveSupply(), true);
        BigInteger numericTarget = CurrencyMinting.getNumericTarget(currency, units);
        json.put("difficulty", String.valueOf(BigInteger.ZERO.equals(numericTarget) ? -1 : BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE).divide(numericTarget)));
        json.put("targetBytes", Convert.toHexString(CurrencyMinting.getTarget(numericTarget)));
        json.put("counter", CurrencyMint.getCounter(currency.getId(), ParameterParser.getAccountId(req, true)));
        return json;
    }

}
