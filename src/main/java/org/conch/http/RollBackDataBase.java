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

import org.conch.db.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.SQLException;

public final class RollBackDataBase extends APIServlet.APIRequestHandler {

    static final RollBackDataBase instance = new RollBackDataBase();

    private RollBackDataBase() {
        super(new APITag[] {APITag.DEBUG}, "scriptFile");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        long timestamp = System.currentTimeMillis();
        JSONObject response = new JSONObject();
        String scriptFile = req.getParameter("scriptFile");
        File file = new File(scriptFile);
        if (!file.exists()) return JSONResponses.fileNotFound(scriptFile);
        try {
            DbRollback.rollback(scriptFile);
        }  catch (SQLException e) {
            e.printStackTrace();
            return JSONResponses.error(e.getMessage());
        }
        response.put("requestProcessingTime", System.currentTimeMillis()-timestamp);
        response.put("rollbacked", true);
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return false;
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
