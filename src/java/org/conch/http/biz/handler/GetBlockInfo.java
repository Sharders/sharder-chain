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
import org.conch.chain.Block;
import org.conch.Conch;
import org.conch.ConchException;
import org.conch.http.*;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Map;

import static org.conch.http.JSONResponses.*;

public final class GetBlockInfo extends APIServlet.APIRequestHandler {

    public static final GetBlockInfo instance = new GetBlockInfo();

    GetBlockInfo() {
        super(new APITag[] {APITag.BIZ}, "height");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Block blockData = null;
        String heightValue = Convert.emptyToNull(req.getParameter("height"));
        if (heightValue == null) {
            throw new ParameterException(BIZ_MISSING_HEIGHT);
        }
        try {
            int height = Integer.parseInt(heightValue);
            if (height < 0 || height > Conch.getBlockchain().getHeight()) {
                return INCORRECT_HEIGHT;
            }
            blockData = Conch.getBlockchain().getBlockAtHeight(height);
        } catch (RuntimeException e) {
            return INCORRECT_HEIGHT;
        }
        String responseStr = JSON.toString(JSONData.block(blockData, true, true));
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject jsonObject = new JSONObject();
            String dtrJson = mapper.writeValueAsString(mapper.readValue(responseStr, org.conch.http.biz.domain.Block.class));
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
