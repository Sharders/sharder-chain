/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.Appendix;
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
            Nxt.getTransactionProcessor().broadcast(transaction);
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
