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

import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.chain.BlockchainImpl;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.conch.http.JSONResponses.*;

public final class GetTransaction extends APIServlet.APIRequestHandler {

    static final GetTransaction instance = new GetTransaction();

    private GetTransaction() {
        super(new APITag[]{APITag.TRANSACTIONS}, "transaction", "fullHash", "includePhasingResult");
    }

    public static List<JSONObject> getTransactions(List<JSONObject> transactions, Boolean includePhasingResult) {
        Transaction transaction;
        if (transactions == null) {
            transactions = new ArrayList<>();
        }

        DbIterator<TransactionImpl> allTransactions = null;
        try{
            allTransactions = BlockchainImpl.getInstance().getAllTransactions();
            while (allTransactions.hasNext()) {
                transaction = allTransactions.next();
                JSONObject transactionJson = JSONData.transaction(transaction, includePhasingResult);
                transactions.add(transactionJson);
            }
        }finally {
            DbUtils.close(allTransactions);
        }
      
        return transactions;
    }

    public static List<JSONObject> getTransactions(String transactionIdString, String transactionFullHash, Boolean includePhasingResult) {
        long transactionId = 0;
        Transaction transaction;
        List<JSONObject> transactions = new ArrayList<>();
        // Id和哈希为空，返回所有的交易列表
        if (StringUtils.isEmpty(transactionIdString) && StringUtils.isEmpty(transactionFullHash)) {
            return null;
        }
        // 精确查询
        // 先查现有区块的交易
        if (StringUtils.isNotEmpty(transactionIdString)) {
            transactionId = Long.parseUnsignedLong(transactionIdString);
            transaction = Conch.getBlockchain().getTransaction(transactionId);
        } else {
            transaction = Conch.getBlockchain().getTransactionByFullHash(transactionFullHash);
            if (transaction == null) {
                return null;
            }
        }

        // 再查未确认的交易
        if (transaction == null) {
            transaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null) {
                return null;
            }
            if (StringUtils.isNotEmpty(transactionFullHash) && !transaction.getFullHash().equalsIgnoreCase(transactionFullHash)) {
                return null;
            }
            transactions.add(JSONData.unconfirmedTransaction(transaction));
            return transactions;
        } else {
            if (StringUtils.isNotEmpty(transactionFullHash) && !transaction.getFullHash().equalsIgnoreCase(transactionFullHash)) {
                return null;
            }
            transactions.add(JSONData.transaction(transaction, includePhasingResult));
            return transactions;
        }
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        String transactionFullHash = Convert.emptyToNull(req.getParameter("fullHash"));
        if (transactionIdString == null && transactionFullHash == null) {
            return MISSING_TRANSACTION;
        }
        boolean includePhasingResult = "true".equalsIgnoreCase(req.getParameter("includePhasingResult"));

        long transactionId = 0;
        Transaction transaction;
        try {
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Conch.getBlockchain().getTransaction(transactionId);
            } else {
                transaction = Conch.getBlockchain().getTransactionByFullHash(transactionFullHash);
                if (transaction == null) {
                    return UNKNOWN_TRANSACTION;
                }
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        if (transaction == null) {
            transaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
            return JSONData.unconfirmedTransaction(transaction);
        } else {
            return JSONData.transaction(transaction, includePhasingResult);
        }

    }

}
