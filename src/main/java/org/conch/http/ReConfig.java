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
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public final class ReConfig extends APIServlet.APIRequestHandler {

    static final ReConfig INSTANCE = new ReConfig();
    static final List<String> excludeParams = Arrays.asList("restart", "requestType", "newAdminPassword", "isInit", "adminPassword", "reBind");
    private ReConfig() {
        super(new APITag[] {APITag.DEBUG}, "restart");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        boolean restart = "true".equalsIgnoreCase(req.getParameter("restart"));
        boolean bindNew = "true".equalsIgnoreCase(req.getParameter("reBind"));
        boolean isInit = "true".equalsIgnoreCase(req.getParameter("isInit"));
        boolean needBind = "true".equalsIgnoreCase(req.getParameter("sharder.HubBind"));
        HashMap map = new HashMap(16);
        Enumeration enu = req.getParameterNames();
        while(enu.hasMoreElements()) {
            String paraName = (String)enu.nextElement();
            if ("newAdminPassword".equals(paraName)) {
                map.put("sharder.adminPassword", req.getParameter(paraName));
                continue;
            }
            if ("sharder.HubBindPassPhrase".equals(paraName) && needBind && !bindNew && !isInit) {
                map.put("sharder.HubBindPassPhrase", Conch.getStringProperty("sharder.HubBindPassPhrase"));
                continue;
            }
            if (excludeParams.contains(paraName)) {
                continue;
            }
            map.put(paraName, req.getParameter(paraName));
        }
        Conch.storePropertiesToFile(map);
        if (restart) {
            new Thread(() -> Conch.restartApplication(null)).start();
        }
        response.put("reconfiged", true);
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
