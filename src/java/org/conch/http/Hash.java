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

import org.conch.crypto.HashFunction;
import org.conch.util.Convert;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class Hash extends APIServlet.APIRequestHandler {

    static final Hash instance = new Hash();

    private Hash() {
        super(new APITag[] {APITag.UTILS}, "hashAlgorithm", "secret", "secretIsText");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        byte algorithm = ParameterParser.getByte(req, "hashAlgorithm", (byte) 0, Byte.MAX_VALUE, false);
        HashFunction hashFunction = null;
        try {
            hashFunction = HashFunction.getHashFunction(algorithm);
        } catch (IllegalArgumentException ignore) {}
        if (hashFunction == null) {
            return JSONResponses.INCORRECT_HASH_ALGORITHM;
        }

        boolean secretIsText = "true".equalsIgnoreCase(req.getParameter("secretIsText"));
        byte[] secret;
        try {
            secret = secretIsText ? Convert.toBytes(req.getParameter("secret"))
                    : Convert.parseHexString(req.getParameter("secret"));
        } catch (RuntimeException e) {
            return JSONResponses.INCORRECT_SECRET;
        }
        if (secret == null || secret.length == 0) {
            return JSONResponses.MISSING_SECRET;
        }

        JSONObject response = new JSONObject();
        response.put("hash", Convert.toHexString(hashFunction.hash(secret)));
        return response;
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
