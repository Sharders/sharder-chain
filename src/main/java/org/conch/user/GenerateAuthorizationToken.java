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

package org.conch.user;

import org.conch.common.Token;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public final class GenerateAuthorizationToken extends UserServlet.UserRequestHandler {

    static final GenerateAuthorizationToken instance = new GenerateAuthorizationToken();

    private GenerateAuthorizationToken() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        String secretPhrase = req.getParameter("secretPhrase");
        if (! user.getSecretPhrase().equals(secretPhrase)) {
            return JSONResponses.INVALID_SECRET_PHRASE;
        }

        String tokenString = Token.generateToken(secretPhrase, req.getParameter("website").trim());

        JSONObject response = new JSONObject();
        response.put("response", "showAuthorizationToken");
        response.put("token", tokenString);

        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
