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

package org.conch.http.biz;

import org.conch.http.*;
import org.conch.http.biz.handler.*;
import org.conch.http.biz.handler.GetBizConstants;

import java.util.*;

public enum BizAPIEnum {
    //To preserve compatibility, please add new APIs to the end of the enum.
    //When an API is deleted, set its name to empty string and handler to null.
    UPLOAD_TEXTDATA("uploadTextData",UploadTextData.instance),
    RETRIEVE_DATA("retrieveTextData",RetrieveTextData.instance),
    CREATE_CLIENT_ACCOUNT("createClientAccount",CreateClientAccount.instance),
    GET_LAST_BLOCK_HEIGHT("getLastBlockHeight",GetLastBlockHeight.instance),
    GET_BLOCK_INFO("getBlockInfo",GetBlockInfo.instance),
    GET_ACCOUNT_INFO("getAccountInfo",GetAccountInfo.instance),
    GET_CONSTANTS("getBizConstants",GetBizConstants.instance),
    GET_BIZ_BLOCKS("getBizBlocks",GetBizBlocks.instance),
    GET_ACCOUNT_TXS("getAccountTxs",GetAccountTxs.instance),
    GET_TX_STATISTIC("getTxStatistics",GetTxStatistics.instance),
    GET_TX("getTx",GetTx.instance);


    private static final Map<String, BizAPIEnum> apiByName = new HashMap<>();
    static {
        List<String> normalAPINames = new ArrayList<>();
        for (APIEnum api : APIEnum.values()) {
            normalAPINames.add(api.getName());
        }
        for (BizAPIEnum api : values()) {
            if (apiByName.put(api.getName(), api) != null || normalAPINames.contains(api.getName())) {
                AssertionError assertionError = new AssertionError("Duplicate BizAPI name: " + api.getName());
                assertionError.printStackTrace();
                throw assertionError;
            }
        }
    }

    private final String name;
    private final APIServlet.APIRequestHandler handler;

    BizAPIEnum(String name, APIServlet.APIRequestHandler handler) {
        this.name = name;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public APIServlet.APIRequestHandler getHandler() {
        return handler;
    }
}
