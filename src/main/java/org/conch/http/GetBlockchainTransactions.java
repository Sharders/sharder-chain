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
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
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
        String recipientRS = req.getParameter("recipientRS");
        String senderRS = req.getParameter("senderRS");
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
        JSONObject response = new JSONObject();
        int count = 0;

        try {
            iterator = Conch.getBlockchain().getTransactions(accountId, numberOfConfirmations,
                    type, subtype, timestamp, withMessage, phasedOnly, nonPhasedOnly, firstIndex, lastIndex,
                    includeExpiredPrunable, executedOnly,recipientRS,senderRS);
            // normal txs
            while (iterator.hasNext()) {
                transactions.add(JSONData.transaction(iterator.next(), includePhasingResult, false));
            }

        }finally {
            DbUtils.close(iterator);
        }
        try{
            if(type != TransactionType.TYPE_POC) {
                count = Conch.getBlockchain().getTransactionCountByAccount(accountId,type,subtype, true, true);
            }
            if(recipientRS!=null||senderRS!=null){
                count = Conch.getBlockchain().getTransactionCountByAccount(accountId,type,subtype, recipientRS, senderRS);
            }

            response.put("count",count);
        }catch(Exception e){
            throw new RuntimeException(e.toString(),e);
        }

        response.put("transactions", transactions);
        return response;

    }

    /**
     * check the poc owner, support:
     * - poc v1 & V2: check the account id from attachement
     * - poc v3: check the recipient id
     * @param accountId
     * @param transaction
     * @return
     */
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

    @Override
    protected boolean startDbTransaction() {
        return true;
    }

    @Override
    protected boolean requireRequestControl() {
        return true;
    }
}
