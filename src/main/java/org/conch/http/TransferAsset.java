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
import org.conch.asset.Asset;
import org.conch.common.ConchException;
import org.conch.tx.Attachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.NOT_ENOUGH_ASSETS;

public final class TransferAsset extends CreateTransaction {

    static final TransferAsset instance = new TransferAsset();

    private TransferAsset() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "recipient", "asset", "quantityQNT");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long recipient = ParameterParser.getAccountId(req, "recipient", true);

        Asset asset = ParameterParser.getAsset(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset.getId(), quantityQNT);
        try {
            return createTransaction(req, account, recipient, 0, attachment);
        } catch (ConchException.InsufficientBalanceException e) {
            return NOT_ENOUGH_ASSETS;
        }
    }

}
