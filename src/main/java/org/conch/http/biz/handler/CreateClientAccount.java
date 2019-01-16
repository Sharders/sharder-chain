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

package org.conch.http.biz.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.crypto.Crypto;
import org.conch.http.*;
import org.conch.http.biz.domain.ErrorDescription;
import org.conch.tools.PassPhraseGenerator;
import org.conch.tx.Attachment;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.conch.http.JSONResponses.BIZ_MISSING_CREATOR_PASSPHRSE;

public final class CreateClientAccount extends CreateTransaction {

    public static final CreateClientAccount instance = new CreateClientAccount();

    private CreateClientAccount() {
        super(new APITag[] {APITag.BIZ, APITag.CREATE_TRANSACTION}, "passPhrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        if (req.getParameter("secretPhrase") == null) {
            throw new ParameterException(BIZ_MISSING_CREATOR_PASSPHRSE);
        }
        String newAccountPassPhrase = PassPhraseGenerator.makeMnemonicWords();
        byte[] publicKey = Crypto.getPublicKey(newAccountPassPhrase);
        long accountId = Account.getId(publicKey);
        long recipientId = Account.getId(publicKey);

        // Send a message to the new account to active it
        Account account = ParameterParser.getSenderAccount(req);
        String createTransactionResponse = JSON.toString(createTransaction(req, account, recipientId, 0, Attachment.ARBITRARY_MESSAGE));

        ObjectMapper mapper = new ObjectMapper();
        ErrorDescription ed = null;
        try {
            ed = mapper.readValue(createTransactionResponse,ErrorDescription.class);
        } catch (IOException e) {
            e.printStackTrace();
            return JSONResponses.BIZ_JSON_IO_ERROR;
        }
        if (ed.getErrorCode() != 0) {
            return ed;
        }
        JSONObject response = new JSONObject();
        response.put("accountID", accountId);
        response.put("accountRS", Account.rsAccount(accountId));
        response.put("publicKey", Convert.toHexString(publicKey));
        response.put("passPhrase", newAccountPassPhrase);
        return response;
    }

}
