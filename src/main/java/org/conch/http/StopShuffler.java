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
import org.conch.shuffle.Shuffler;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public final class StopShuffler extends APIServlet.APIRequestHandler {

    static final StopShuffler instance = new StopShuffler();

    private StopShuffler() {
        super(new APITag[] {APITag.SHUFFLING}, "account", "shufflingFullHash", "secretPhrase", "adminPassword");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        byte[] shufflingFullHash = ParameterParser.getBytes(req, "shufflingFullHash", false);
        long accountId = ParameterParser.getAccountId(req, false);
        JSONObject response = new JSONObject();
        if (secretPhrase != null) {
            if (accountId != 0 && Account.getId(Crypto.getPublicKey(secretPhrase)) != accountId) {
                return JSONResponses.INCORRECT_ACCOUNT;
            }
            accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
            if (shufflingFullHash.length == 0) {
                return JSONResponses.missing("shufflingFullHash");
            }
            Shuffler shuffler = Shuffler.stopShuffler(accountId, shufflingFullHash);
            response.put("stoppedShuffler", shuffler != null);
        } else {
            API.verifyPassword(req);
            if (accountId != 0 && shufflingFullHash.length != 0) {
                Shuffler shuffler = Shuffler.stopShuffler(accountId, shufflingFullHash);
                response.put("stoppedShuffler", shuffler != null);
            } else if (accountId == 0 && shufflingFullHash.length == 0) {
                Shuffler.stopAllShufflers();
                response.put("stoppedAllShufflers", true);
            } else if (accountId != 0) {
                return JSONResponses.missing("shufflingFullHash");
            } else if (shufflingFullHash.length != 0) {
                return JSONResponses.missing("account");
            }
        }
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
    protected boolean requireFullClient() {
        return true;
    }

}
