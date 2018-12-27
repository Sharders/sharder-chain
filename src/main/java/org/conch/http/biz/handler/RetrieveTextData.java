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
import org.conch.common.ConchException;
import org.conch.http.*;
import org.conch.http.biz.domain.Data;
import org.conch.storage.TaggedData;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

import static org.conch.http.JSONResponses.PRUNED_TRANSACTION;

public final class RetrieveTextData extends APIServlet.APIRequestHandler {

    public static final RetrieveTextData instance = new RetrieveTextData();

    RetrieveTextData() {
        super(new APITag[] {APITag.BIZ},"txID");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        long transactionId = ParameterParser.getUnsignedLong(req, "txID", true);
        String taggedDataJsonStr = "";
        TaggedData taggedData = TaggedData.getData(transactionId);
        if (taggedData == null) {
            if (Conch.getBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                return PRUNED_TRANSACTION;
            }
            taggedData = TaggedData.getData(transactionId);
        }
        if (taggedData != null) {
            taggedDataJsonStr = JSON.toString(JSONData.taggedData(taggedData, true));
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject jsonObject = new JSONObject();
            String dtrJson = mapper.writeValueAsString(mapper.readValue(taggedDataJsonStr, Data.class));
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
