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
import org.conch.Transaction;
import org.conch.http.APIServlet;
import org.conch.http.APITag;
import org.conch.http.JSONData;
import org.conch.http.JSONResponses;
import org.conch.http.biz.domain.Data;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Map;

import static org.conch.http.JSONResponses.*;

public final class GetTx extends APIServlet.APIRequestHandler {

    public static final GetTx instance = new GetTx();

    private GetTx() {
        super(new APITag[] {APITag.BIZ}, "txID", "hash");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionIdString = Convert.emptyToNull(req.getParameter("txID"));
        String transactionFullHash = Convert.emptyToNull(req.getParameter("hash"));
        if (transactionIdString == null && transactionFullHash == null) {
            return MISSING_TRANSACTION;
        }

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

        JSONObject txJson;
        if (transaction == null) {
            transaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
            txJson = JSONData.unconfirmedTransaction(transaction);
        } else {
            txJson = JSONData.transaction(transaction, false);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject jsonObject = new JSONObject();
            String dtrJson = mapper.writeValueAsString(mapper.readValue(JSON.toJSONString(txJson), org.conch.http.biz.domain.Transaction.class));
            Map<String, Object> map = mapper.readValue(dtrJson, new TypeReference<Map<String, Object>>(){});
            jsonObject.putAll(map);
            return jsonObject;
        } catch (IOException e) {
            e.printStackTrace();
            return JSONResponses.BIZ_JSON_IO_ERROR;
        }
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
