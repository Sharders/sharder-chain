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
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.http.APITag;
import org.conch.http.CreateTransaction;
import org.conch.http.JSONResponses;
import org.conch.http.ParameterParser;
import org.conch.http.biz.BizParameterParser;
import org.conch.http.biz.domain.DataTransactionResponse;
import org.conch.tx.Attachment;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

public final class StoreTextData extends CreateTransaction {

    public static final StoreTextData instance = new StoreTextData();

    StoreTextData() {
        super(new APITag[] {APITag.DATA_STORAGE, APITag.CREATE_TRANSACTION},
                "passPhrase", "fileType", "clientAccount", "fileName", "data");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        Account account = ParameterParser.getSenderAccount(req);
        Attachment.DataStorageUpload storageCache = BizParameterParser.storeCache(req);

        String createTransactionResponse = JSON.toString(createTransaction(req, account, storageCache));
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
