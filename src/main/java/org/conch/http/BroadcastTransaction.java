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

import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.tx.Appendix;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * The purpose of broadcast transaction is to support client side signing of transactions.
 * Clients first submit their transaction using {@link CreateTransaction} without providing the secret phrase.<br>
 * In response the client receives the unsigned transaction JSON and transaction bytes.
 * <p>
 * The client then signs and submits the signed transaction using {@link BroadcastTransaction}
 * <p>
 * The default wallet implements this procedure in nrs.server.js which you can use as reference.
 * <p>
 * {@link BroadcastTransaction} accepts the following parameters:<br>
 * transactionJSON - JSON representation of the signed transaction<br>
 * transactionBytes - row bytes composing the signed transaction bytes excluding the prunable appendages<br>
 * prunableAttachmentJSON - JSON representation of the prunable appendages<br>
 * <p>
 * Clients can submit either the signed transactionJSON or the signed transactionBytes but not both.<br>
 * In case the client submits transactionBytes for a transaction containing prunable appendages, the client also needs
 * to submit the prunableAttachmentJSON parameter which includes the attachment JSON for the prunable appendages.<br>
 * <p>
 * Prunable appendages are classes implementing the {@link Appendix.Prunable} interface.
 */
public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionJSON", "transactionBytes", "prunableAttachmentJSON");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));

        JSONObject response = new JSONObject();
        try {
            Transaction.Builder builder = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON);
            Transaction transaction = builder.build();
            Conch.getTransactionProcessor().broadcast(transaction);
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHash());
        } catch (ConchException.ValidationException |RuntimeException e) {
            JSONData.putException(response, e, "Failed to broadcast transaction");
        }
        return response;

    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected final boolean allowRequiredBlockParameters() {
        return false;
    }

}
