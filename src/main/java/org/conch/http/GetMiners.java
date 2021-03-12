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

import org.conch.account.Account;
import org.conch.crypto.Crypto;
import org.conch.mint.Generator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.*;


/**
 * @author ben-xy
 */
public final class GetMiners extends APIServlet.APIRequestHandler {

    static final GetMiners instance = new GetMiners();

    private GetMiners() {
        super(new APITag[]{APITag.FORGING}, "secretPhrase", "adminPassword", "signature", "message");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        StartForging instance = StartForging.instance;
        String pr;
        try {
            pr = instance.verifySignature(req);
        } catch (Exception e) {
            return JSONResponses.error(e.getMessage());
        }
        boolean loadPoolInfo = ParameterParser.getBoolean(req, "loadPoolInfo");
        if (pr != null) {
            Account account = Account.getAccount(Crypto.getPublicKey(pr));
            if (account == null) {
                return UNKNOWN_ACCOUNT;
            }
            Generator generator = Generator.getGenerator(pr);
            if (generator == null) {
                return NOT_FORGING;
            }
            return generator.toJson(loadPoolInfo);
        } else {
            API.verifyPassword(req);
            JSONObject response = new JSONObject();
            JSONArray generators = new JSONArray();
            Generator.getSortedMiners().forEach(generator -> generators.add(generator.toJson(loadPoolInfo)));
            response.put("generators", generators);
            return response;
        }

    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

}
