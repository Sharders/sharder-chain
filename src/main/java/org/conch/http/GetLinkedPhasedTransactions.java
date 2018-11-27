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

import org.conch.common.ConchException;
import org.conch.tx.Transaction;
import org.conch.vote.PhasingPoll;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class GetLinkedPhasedTransactions extends APIServlet.APIRequestHandler {
    static final GetLinkedPhasedTransactions instance = new GetLinkedPhasedTransactions();

    private GetLinkedPhasedTransactions() {
        super(new APITag[]{APITag.PHASING}, "linkedFullHash");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        byte[] linkedFullHash = ParameterParser.getBytes(req, "linkedFullHash", true);

        JSONArray json = new JSONArray();
        List<? extends Transaction> transactions = PhasingPoll.getLinkedPhasedTransactions(linkedFullHash);
        transactions.forEach(transaction -> json.add(JSONData.transaction(transaction)));
        JSONObject response = new JSONObject();
        response.put("transactions", json);

        return response;
    }
}