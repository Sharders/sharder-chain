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

import org.conch.Conch;
import org.conch.consensus.poc.hardware.PerformanceCheckingUtil;
import org.conch.util.IpUtil;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPerformance extends APIServlet.APIRequestHandler {

    static final GetPerformance instance = new GetPerformance();

    private GetPerformance() {
        super(new APITag[] {APITag.TEST}, "time");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) {
        String senderIp = IpUtil.getSenderIp(request);
        String foundationIp = IpUtil.getIp(Conch.getSharderFoundationURL());
        
        if (!foundationIp.equals(senderIp)) return null;
        
        Integer time = Integer.parseInt(request.getParameter("time"));
        JSONObject response = new JSONObject();
        response.put("executeCount", PerformanceCheckingUtil.check(time));
        return response;
    }

}
