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

package org.conch.http.biz.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.conch.Conch;
import org.conch.ConchException;
import org.conch.Transaction;
import org.conch.db.DbIterator;
import org.conch.http.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GetAccountTxs extends APIServlet.APIRequestHandler {

    public static final GetAccountTxs instance = new GetAccountTxs();

    private GetAccountTxs() {
        super(new APITag[] {APITag.BIZ}, "account","firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long accountId = ParameterParser.getAccountId(req, true);
        int timestamp = ParameterParser.getTimestamp(req);
        int numberOfConfirmations = ParameterParser.getNumberOfConfirmations(req);

        byte type = -1;
        byte subtype = -1;
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator = Conch.getBlockchain().getTransactions(accountId, numberOfConfirmations,
                type, subtype, timestamp, false, false, false, firstIndex, lastIndex,
                false, false)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(transaction, false));
            }
        }

        JSONArray response = new JSONArray();
        ObjectMapper mapper = new ObjectMapper();
        try {
            String dtrJson = mapper.writeValueAsString(mapper.readValue(transactions.toJSONString(),new TypeReference<ArrayList<org.conch.http.biz.domain.Transaction>>(){}));
            ArrayList list = mapper.readValue(dtrJson, new TypeReference<List<Map<String, Object>>>(){});
            response.addAll(list);
        } catch (IOException e) {
            e.printStackTrace();
            return JSONResponses.BIZ_JSON_IO_ERROR;
        }
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
