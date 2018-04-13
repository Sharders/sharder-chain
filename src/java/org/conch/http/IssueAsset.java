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

import org.conch.Account;
import org.conch.Attachment;
import org.conch.Constants;
import org.conch.ConchException;
import org.conch.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static org.conch.http.JSONResponses.INCORRECT_ASSET_NAME;
import static org.conch.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static org.conch.http.JSONResponses.INCORRECT_DECIMALS;
import static org.conch.http.JSONResponses.MISSING_NAME;

public final class IssueAsset extends CreateTransaction {

    static final IssueAsset instance = new IssueAsset();

    private IssueAsset() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "name", "description", "quantityQNT", "decimals");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String decimalsValue = Convert.emptyToNull(req.getParameter("decimals"));

        if (name == null) {
            return MISSING_NAME;
        }

        name = name.trim();
        if (name.length() < Constants.MIN_ASSET_NAME_LENGTH || name.length() > Constants.MAX_ASSET_NAME_LENGTH) {
            return INCORRECT_ASSET_NAME_LENGTH;
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return INCORRECT_ASSET_NAME;
            }
        }

        if (description != null && description.length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH) {
            return INCORRECT_ASSET_DESCRIPTION;
        }

        byte decimals = 0;
        if (decimalsValue != null) {
            try {
                decimals = Byte.parseByte(decimalsValue);
                if (decimals < 0 || decimals > 8) {
                    return INCORRECT_DECIMALS;
                }
            } catch (NumberFormatException e) {
                return INCORRECT_DECIMALS;
            }
        }

        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantityQNT, decimals);
        return createTransaction(req, account, attachment);

    }

}
