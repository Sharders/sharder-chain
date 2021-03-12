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

import org.conch.crypto.Crypto;
import org.conch.mint.Generator;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public final class StartForging extends APIServlet.APIRequestHandler {

    static final StartForging instance = new StartForging();

    private StartForging() {
        super(new APITag[] {APITag.FORGING}, "signature", "message");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String pr = null;
        try {
            pr = verifySignature(req);
        } catch (Exception e) {
            return JSONResponses.error(e.getMessage());
        }
        Generator generator = Generator.startMining(pr);
        JSONObject response = new JSONObject();
        if(generator != null){
            response.put("deadline", generator.getDeadline());
            response.put("hitTime", generator.getHitTime());
        } else {
            return JSONResponses.error("can't start mining, check the account balance and other mining conditions");
        }
        return response;

    }

    public String verifySignature (HttpServletRequest req) {
        String signature = req.getParameter("signature");
        String message = req.getParameter("message");
        String miningPR = Generator.getAutoMiningPR();
        if (miningPR != null && signature != null && message != null) {
            // Signature Validation
            if (!Crypto.verify(Convert.parseHexString(signature), message.getBytes(), Crypto.getPublicKey(miningPR), true)) {
                throw new RuntimeException("can't start mining, signature verify failed");
            }
            return miningPR;
        }
        return null;
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
    protected boolean requireFullClient() {
        return true;
    }

}
