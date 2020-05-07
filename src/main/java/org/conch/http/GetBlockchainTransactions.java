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
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.db.*;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBlockchainTransactions extends APIServlet.APIRequestHandler {

    static final GetBlockchainTransactions instance = new GetBlockchainTransactions();

    private GetBlockchainTransactions() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS},  "account", "timestamp", "type", "subtype",
                "firstIndex", "lastIndex", "numberOfConfirmations", "withMessage", "phasedOnly", "nonPhasedOnly",
                "includeExpiredPrunable", "includePhasingResult", "executedOnly");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long accountId = ParameterParser.getAccountId(req, true);
        int timestamp = ParameterParser.getTimestamp(req);
        int numberOfConfirmations = ParameterParser.getNumberOfConfirmations(req);
        boolean withMessage = "true".equalsIgnoreCase(req.getParameter("withMessage"));
        boolean phasedOnly = "true".equalsIgnoreCase(req.getParameter("phasedOnly"));
        boolean nonPhasedOnly = "true".equalsIgnoreCase(req.getParameter("nonPhasedOnly"));
        boolean includeExpiredPrunable = "true".equalsIgnoreCase(req.getParameter("includeExpiredPrunable"));
        boolean includePhasingResult = "true".equalsIgnoreCase(req.getParameter("includePhasingResult"));
        boolean executedOnly = "true".equalsIgnoreCase(req.getParameter("executedOnly"));

        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter("type"));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter("subtype"));
        } catch (NumberFormatException e) {
            subtype = -1;
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        JSONArray transactions = new JSONArray();
        DbIterator<? extends Transaction> iterator = null;
        try {

            long statementAccountId = new Long(accountId);
            iterator = Conch.getBlockchain().getTransactions(accountId, numberOfConfirmations,
                    type, subtype, timestamp, withMessage, phasedOnly, nonPhasedOnly, firstIndex, lastIndex,
                    includeExpiredPrunable, executedOnly);
            // normal txs
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                // Poc statement(PoC Node Type Tx)
                if(TransactionType.TYPE_POC == type) {
                    if(isBelongToAccount(statementAccountId, transaction)){
                        transactions.add(JSONData.transaction(transaction, includePhasingResult));
                    }
                }else{
                    transactions.add(JSONData.transaction(transaction, includePhasingResult));
                }
            }

            // genesis txs process
            SharderGenesis.nodeTypeTxs().forEach(tx -> {
                tx.setIndex(0);
                // Poc statement(PoC Node Type Tx)
                if(TransactionType.TYPE_POC == tx.getType().getType()) {
                    if(isBelongToAccount(statementAccountId, tx)) {
                        transactions.add(JSONData.transaction(tx, includePhasingResult));
                    }
                }else{
                    transactions.add(JSONData.transaction(tx, includePhasingResult));
                }
            });
        }finally {
            DbUtils.close(iterator);
        }

        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
//        response.put("count", count);
        return response;

    }

    private static boolean isBelongToAccount(long accountId, Transaction transaction){
        Attachment attachment = transaction.getAttachment();
        if(PocTxWrapper.SUBTYPE_POC_NODE_TYPE == attachment.getTransactionType().getSubtype()) {
            long accountIdOfAttachment = -1L;
            if(attachment instanceof PocTxBody.PocNodeTypeV3){
                accountIdOfAttachment = ((PocTxBody.PocNodeTypeV3) attachment).getAccountId();
            }else if(attachment instanceof PocTxBody.PocNodeTypeV2){
                accountIdOfAttachment = ((PocTxBody.PocNodeTypeV2) attachment).getAccountId();
            }

            if(accountId != accountIdOfAttachment) {
                return false;
            }
        }
        return true;
    }

}
