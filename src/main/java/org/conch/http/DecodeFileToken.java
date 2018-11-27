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

import org.conch.common.Token;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

public final class DecodeFileToken extends APIServlet.APIRequestHandler {

    static final DecodeFileToken instance = new DecodeFileToken();

    private DecodeFileToken() {
        super("file", new APITag[] {APITag.TOKENS}, "token");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String tokenString = req.getParameter("token");
        if (tokenString == null) {
            return JSONResponses.MISSING_TOKEN;
        }
        byte[] data;
        try {
            Part part = req.getPart("file");
            if (part == null) {
                throw new ParameterException(JSONResponses.INCORRECT_FILE);
            }
            ParameterParser.FileData fileData = new ParameterParser.FileData(part).invoke();
            data = fileData.getData();
        } catch (IOException | ServletException e) {
            throw new ParameterException(JSONResponses.INCORRECT_FILE);
        }

        try {
            Token token = Token.parseToken(tokenString, data);
            return JSONData.token(token);
        } catch (RuntimeException e) {
            return JSONResponses.INCORRECT_TOKEN;
        }
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
