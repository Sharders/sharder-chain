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

import org.conch.Conch;
import org.conch.http.*;
import org.conch.util.Convert;
import org.h2.util.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public final class GetBizBlockNum extends APIServlet.APIRequestHandler {

    public static final GetBizBlockNum instance = new GetBizBlockNum();

    private GetBizBlockNum() {
        super(new APITag[] {APITag.BIZ},  "includeTypes");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req){
        long count=0;
        String includeType  = Convert.emptyToNull(req.getParameter("includeTypes"));
        if(includeType!=null){
            List<String> typeList = Arrays.asList(StringUtils.arraySplit(includeType, ',', true));
            count = Conch.getBlockchain().countIncludeTypeBlocks(typeList);
        }
        JSONObject response = new JSONObject();
        response.put("count", count);
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
