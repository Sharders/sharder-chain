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

import org.conch.ConchException;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.Search;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

public final class DetectMimeType extends APIServlet.APIRequestHandler {

    static final DetectMimeType instance = new DetectMimeType();

    private DetectMimeType() {
        super("file", new APITag[] {APITag.DATA, APITag.UTILS}, "data", "filename", "isText");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        String filename = Convert.nullToEmpty(req.getParameter("filename")).trim();
        String dataValue = Convert.emptyToNull(req.getParameter("data"));
        byte[] data;
        if (dataValue == null) {
            try {
                Part part = req.getPart("file");
                if (part == null) {
                    throw new ParameterException(JSONResponses.INCORRECT_TAGGED_DATA_FILE);
                }
                ParameterParser.FileData fileData = new ParameterParser.FileData(part).invoke();
                data = fileData.getData();
                // Depending on how the client submits the form, the filename, can be a regular parameter
                // or encoded in the multipart form. If its not a parameter we take from the form
                if (filename.isEmpty() && fileData.getFilename() != null) {
                    filename = fileData.getFilename();
                }
            } catch (IOException | ServletException e) {
                Logger.logDebugMessage("error in reading file data", e);
                throw new ParameterException(JSONResponses.INCORRECT_TAGGED_DATA_FILE);
            }
        } else {
            boolean isText = !"false".equalsIgnoreCase(req.getParameter("isText"));
            data = isText ? Convert.toBytes(dataValue) : Convert.parseHexString(dataValue);
        }

        JSONObject response = new JSONObject();
        response.put("type", Search.detectMimeType(data, filename));
        return response;
    }

    @Override
    protected boolean requirePost() {
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
