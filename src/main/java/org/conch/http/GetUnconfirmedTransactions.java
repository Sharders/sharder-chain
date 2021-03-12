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
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.db.FilteringIterator;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionProcessorImpl;
import org.conch.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

public final class GetUnconfirmedTransactions extends APIServlet.APIRequestHandler {

    static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();

    private GetUnconfirmedTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Set<Long> accountIds = Convert.toSet(ParameterParser.getAccountIds(req, false));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        JSONObject response = new JSONObject();

  
        try{
            Conch.getBlockchain().readLock();
            JSONArray transactions = new JSONArray();
            if (accountIds.isEmpty()) {
                DbIterator<? extends Transaction> transactionsIterator = null;
                try {
                    transactionsIterator = Conch.getTransactionProcessor().getAllUnconfirmedTransactions(firstIndex, lastIndex);
                    while (transactionsIterator.hasNext()) {
                        Transaction transaction = transactionsIterator.next();
                        transactions.add(JSONData.unconfirmedTransaction(transaction));
                    }
                }finally {
                    DbUtils.close(transactionsIterator);
                }
              
            } else {
                FilteringIterator<? extends Transaction> transactionsIterator = null;
                try {
                    transactionsIterator = new FilteringIterator<> (
                            Conch.getTransactionProcessor().getAllUnconfirmedTransactions(),
                            transaction -> accountIds.contains(transaction.getSenderId()) || accountIds.contains(transaction.getRecipientId()),
                            firstIndex, lastIndex);
                    while (transactionsIterator.hasNext()) {
                        Transaction transaction = transactionsIterator.next();
                        transactions.add(JSONData.unconfirmedTransaction(transaction));
                    }
                }finally {
                    DbUtils.close(transactionsIterator);
                }
            }
            response.put("unconfirmedTransactions", transactions);
        } catch(Exception e) {
            TransactionProcessorImpl.getInstance().processDirtyOrViciousTx(e);
            response.put("error", e.getMessage());
        } finally {
            Conch.getBlockchain().readUnlock();
        }
        return response;
    }
}
