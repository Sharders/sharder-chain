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
import org.conch.chain.Block;
import org.conch.common.ConchException;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.http.*;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.h2.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.conch.http.JSONResponses.BIZ_INCORRECT_INDEX;

public final class GetBizBlocks extends APIServlet.APIRequestHandler {

    public static final GetBizBlocks instance = new GetBizBlocks();

    private GetBizBlocks() {
        super(new APITag[] {APITag.BIZ}, "firstIndex", "lastIndex", "includeTypes");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        String includeType  = Convert.emptyToNull(req.getParameter("includeTypes"));
        if (includeType == null && lastIndex-firstIndex > 500) {
            throw new ParameterException(BIZ_INCORRECT_INDEX);
        }
        final int timestamp = ParameterParser.getTimestamp(req);
        JSONArray blocks = new JSONArray();
        DbIterator<? extends Block> iterator = null;
        try {
            iterator = Conch.getBlockchain().getBlocks(firstIndex, lastIndex);
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getTimestamp() < timestamp) {
                    break;
                }
                if (!StringUtils.isNullOrEmpty(includeType)) {
                    List<String> typeList = Arrays.asList(StringUtils.arraySplit(includeType, ',', true));
                    List<Transaction> transactionList = block.getTransactions().stream().filter(
                            transaction -> typeList.contains(String.valueOf(transaction.getType().getType()))
                    ).collect(Collectors.toList());
                    if (transactionList.size() <= 0) {
                        continue;
                    }
                }
                JSONObject blockJson = JSONData.block(block, true, false);
                blocks.add(blockJson);
            }
        }finally {
            DbUtils.close(iterator);
        }
        JSONArray response = new JSONArray();
        ObjectMapper mapper = new ObjectMapper();
        try {
            String dtrJson = mapper.writeValueAsString(mapper.readValue(blocks.toJSONString(),new TypeReference<ArrayList<org.conch.http.biz.domain.Block>>(){}));
            ArrayList list = mapper.readValue(dtrJson, new TypeReference<List<Map<String, Object>>>(){});
            response.addAll(list);
        } catch (IOException e) {
            Logger.logErrorMessage("can't parse blocks detail in GetBizBlocks Api process");
            return JSONResponses.BIZ_JSON_IO_ERROR;
        }

        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
