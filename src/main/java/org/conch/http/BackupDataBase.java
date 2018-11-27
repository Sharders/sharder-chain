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

import org.conch.db.DbBackupTask;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class BackupDataBase extends APIServlet.APIRequestHandler {

    static final BackupDataBase instance = new BackupDataBase();

    private BackupDataBase() {
        super(new APITag[] {APITag.DEBUG}, "path","fileName");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        long timestamp = System.currentTimeMillis();
        JSONObject response = new JSONObject();
        String path = req.getParameter("path");
        String fileName = req.getParameter("fileName");
        String backupFile = DbBackupTask.execute(path, fileName);
        response.put("requestProcessingTime", System.currentTimeMillis()-timestamp);
        response.put("backupFile", backupFile);
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
