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
import org.conch.Account;
import org.conch.Attachment;
import org.conch.ConchException;
import org.conch.http.*;
import org.conch.http.biz.BizParameterParser;
import org.conch.http.biz.domain.DataTransactionResponse;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

import static org.conch.http.JSONResponses.BIZ_MISSING_CLIENT;
public final class UploadTextData extends CreateTransaction {

    public static final UploadTextData instance = new UploadTextData();

    UploadTextData() {
        super(new APITag[] {APITag.BIZ, APITag.CREATE_TRANSACTION},
                "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        if (req.getParameter("clientAccount") == null) {
            throw new ParameterException(BIZ_MISSING_CLIENT);
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment.TaggedDataUpload taggedDataUpload = BizParameterParser.getTextData(req);

        String createTransactionResponse = JSON.toString(createTransaction(req, account, taggedDataUpload));
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject jsonObject = new JSONObject();
            String dtrJson = mapper.writeValueAsString(mapper.readValue(createTransactionResponse, DataTransactionResponse.class));
            Map<String, Object> map = mapper.readValue(dtrJson, new TypeReference<Map<String, Object>>(){});
            jsonObject.putAll(map);
            return jsonObject;
        } catch (IOException e) {
            e.printStackTrace();
            return JSONResponses.BIZ_JSON_IO_ERROR;
        }

    }
}
