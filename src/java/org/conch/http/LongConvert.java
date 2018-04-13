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

import org.conch.util.Convert;
import org.conch.util.JSON;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

public final class LongConvert extends APIServlet.APIRequestHandler {

    static final LongConvert instance = new LongConvert();

    private LongConvert() {
        super(new APITag[] {APITag.UTILS}, "id");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        String id = Convert.emptyToNull(req.getParameter("id"));
        if (id == null) {
            return JSON.emptyJSON;
        }
        JSONObject response = new JSONObject();
        BigInteger bigInteger = new BigInteger(id);
        if (bigInteger.signum() < 0) {
            if (bigInteger.negate().compareTo(Convert.two64) > 0) {
                return JSONResponses.OVERFLOW;
            } else {
                response.put("stringId", bigInteger.add(Convert.two64).toString());
                response.put("longId", String.valueOf(bigInteger.longValue()));
            }
        } else {
            if (bigInteger.compareTo(Convert.two64) >= 0) {
                return JSONResponses.OVERFLOW;
            } else {
                response.put("stringId", bigInteger.toString());
                response.put("longId", String.valueOf(bigInteger.longValue()));
            }
        }
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
