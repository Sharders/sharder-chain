/*
 * Copyright © 2017 sharder.org.
 * Copyright © 2014-2017 ichaoj.com.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,
 * no part of the COS software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package org.conch.peer;

import org.conch.Blockchain;
import org.conch.Constants;
import org.conch.Conch;
import org.conch.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * Get the transactions
 */
public class GetTransactions extends PeerServlet.PeerRequestHandler {

    static final GetTransactions instance = new GetTransactions();

    private GetTransactions() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        if (!Constants.INCLUDE_EXPIRED_PRUNABLE) {
            return PeerServlet.UNSUPPORTED_REQUEST_TYPE;
        }
        JSONObject response = new JSONObject();
        JSONArray transactionArray = new JSONArray();
        JSONArray transactionIds = (JSONArray)request.get("transactionIds");
        Blockchain blockchain = Conch.getBlockchain();
        //
        // Return the transactions to the caller
        //
        if (transactionIds != null) {
            transactionIds.forEach(transactionId -> {
                long id = Long.parseUnsignedLong((String)transactionId);
                Transaction transaction = blockchain.getTransaction(id);
                if (transaction != null) {
                    transaction.getAppendages(true);
                    JSONObject transactionJSON = transaction.getJSONObject();
                    transactionArray.add(transactionJSON);
                }
            });
        }
        response.put("transactions", transactionArray);
        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }
}
