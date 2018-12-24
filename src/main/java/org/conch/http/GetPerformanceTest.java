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

import org.conch.util.IPList;
import org.conch.util.PerformanceTestUtil;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPerformanceTest extends APIServlet.APIRequestHandler {

    static final GetPerformanceTest instance = new GetPerformanceTest();

    private GetPerformanceTest() {
        super(new APITag[] {APITag.TEST}, "time");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) {
        String senderIp;
        if (request.getHeader("x-forwarded-for") == null) {
            senderIp =  request.getRemoteAddr();
        } else {
            senderIp =  request.getHeader("x-forwarded-for");
        }
        if (IPList.SERVER_IP.equals(senderIp)){
            String time = request.getParameter("time");
            Long executeCount = PerformanceTestUtil.test(Integer.parseInt(time));
            JSONObject response = new JSONObject();
            response.put("executeCount", executeCount);
            return response;
        }
//        String time = request.getParameter("time");
//        Long executeCount = PerformanceTestUtil.test(Integer.parseInt(time));
//        JSONObject response = new JSONObject();
//        response.put("executeCount", executeCount);
        return null;
    }

}
