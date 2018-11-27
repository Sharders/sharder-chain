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
import org.conch.market.Order;
import org.conch.tx.Attachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.UNKNOWN_ORDER;

public final class CancelAskOrder extends CreateTransaction {

    static final CancelAskOrder instance = new CancelAskOrder();

    private CancelAskOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "order");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        long orderId = ParameterParser.getUnsignedLong(req, "order", true);
        Account account = ParameterParser.getSenderAccount(req);
        Order.Ask orderData = Order.Ask.getAskOrder(orderId);
        if (orderData == null || orderData.getAccountId() != account.getId()) {
            return UNKNOWN_ORDER;
        }
        Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(orderId);
        return createTransaction(req, account, attachment);
    }

}
