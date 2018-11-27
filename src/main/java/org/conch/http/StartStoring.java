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

import org.conch.Constants;
import org.conch.account.Account;
import org.conch.crypto.Crypto;
import org.conch.storage.Storer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.*;


public final class StartStoring extends APIServlet.APIRequestHandler {

    static final StartStoring instance = new StartStoring();

    private StartStoring() {
        super(new APITag[] {APITag.DATA_STORAGE}, "secretPhrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        if (!Constants.isStorageClient) {
            return NOT_ENABLE_STORAGE;
        }
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }
        Storer storer = Storer.startStoring(secretPhrase);
        if (storer == null) {
            return NOT_STORING;
        }
        JSONObject response = new JSONObject();
        response.put("storerId", Long.toUnsignedString(storer.getAccountId()));
        response.put("status", storer.toString());
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
        return false;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }
}
