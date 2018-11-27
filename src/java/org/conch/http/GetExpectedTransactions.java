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
import org.conch.ConchException;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

public final class GetExpectedTransactions extends APIServlet.APIRequestHandler {

    static final GetExpectedTransactions instance = new GetExpectedTransactions();

    private GetExpectedTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS}, "account", "account", "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        Set<Long> accountIds = Convert.toSet(ParameterParser.getAccountIds(req, false));
        Filter<Transaction> filter = accountIds.isEmpty() ? transaction -> true :
                transaction -> accountIds.contains(transaction.getSenderId()) || accountIds.contains(transaction.getRecipientId());

        List<? extends Transaction> transactions = Conch.getBlockchain().getExpectedTransactions(filter);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        transactions.forEach(transaction -> jsonArray.add(JSONData.unconfirmedTransaction(transaction)));
        response.put("expectedTransactions", jsonArray);

        return response;
    }

}
