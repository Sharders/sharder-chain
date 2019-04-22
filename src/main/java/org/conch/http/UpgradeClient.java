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

import org.apache.commons.lang3.StringUtils;
import org.conch.tools.ClientUpgradeTool;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * @author jiangbubai
 */
public final class UpgradeClient extends APIServlet.APIRequestHandler {

    static final UpgradeClient INSTANCE = new UpgradeClient();

    private UpgradeClient() {
        super(new APITag[] {APITag.DEBUG}, "version", "mode" , "restart");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        boolean restart = "true".equalsIgnoreCase(req.getParameter("restart"));
        String version = Convert.emptyToNull(req.getParameter("version"));
        String mode = Convert.emptyToNull(req.getParameter("mode"));
        String bakMode = Convert.emptyToNull(req.getParameter("bakMode"));
        
        // set default value
        if(StringUtils.isEmpty(mode)) mode = ClientUpgradeTool.VER_MODE_INCREMENTAL;
        if(StringUtils.isEmpty(bakMode)) bakMode = ClientUpgradeTool.BAK_MODE_DELETE;
        
        if (StringUtils.isEmpty(version)) {
            response.put("upgraded", false);
            response.put("error", "version can not be null");
            return response;
        }
        ClientUpgradeTool.upgradePackageThread(version,mode,bakMode,restart);
        response.put("upgraded", true);
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
