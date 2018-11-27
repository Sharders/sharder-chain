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

import org.conch.Appendix;
import org.conch.Conch;
import org.conch.ConchException;
import org.conch.tx.Transaction;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static org.conch.http.JSONResponses.EITHER_MESSAGE_ENCRYPTED_MESSAGE;
import static org.conch.http.JSONResponses.MISSING_MESSAGE_ENCRYPTED_MESSAGE;
import static org.conch.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class VerifyPrunableMessage extends APIServlet.APIRequestHandler {

    static final VerifyPrunableMessage instance = new VerifyPrunableMessage();

    private static final JSONStreamAware NO_SUCH_PLAIN_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no plain message attachment");
        NO_SUCH_PLAIN_MESSAGE = JSON.prepare(response);
    }

    private static final JSONStreamAware NO_SUCH_ENCRYPTED_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no encrypted message attachment");
        NO_SUCH_ENCRYPTED_MESSAGE = JSON.prepare(response);
    }

    private VerifyPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction",
                "message", "messageIsText",
                "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "compressMessageToEncrypt");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        Transaction transaction = Conch.getBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }

        Appendix.PrunablePlainMessage plainMessage = (Appendix.PrunablePlainMessage) ParameterParser.getPlainMessage(req, true);
        Appendix.PrunableEncryptedMessage encryptedMessage = (Appendix.PrunableEncryptedMessage) ParameterParser.getEncryptedMessage(req, null, true);

        if (plainMessage == null && encryptedMessage == null) {
            return MISSING_MESSAGE_ENCRYPTED_MESSAGE;
        }
        if (plainMessage != null && encryptedMessage != null) {
            return EITHER_MESSAGE_ENCRYPTED_MESSAGE;
        }

        if (plainMessage != null) {
            Appendix.PrunablePlainMessage myPlainMessage = transaction.getPrunablePlainMessage();
            if (myPlainMessage == null) {
                return NO_SUCH_PLAIN_MESSAGE;
            }
            if (!Arrays.equals(myPlainMessage.getHash(), plainMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myPlainMessage.getJSONObject();
            response.put("verify", true);
            return response;
        } else if (encryptedMessage != null) {
            Appendix.PrunableEncryptedMessage myEncryptedMessage = transaction.getPrunableEncryptedMessage();
            if (myEncryptedMessage == null) {
                return NO_SUCH_ENCRYPTED_MESSAGE;
            }
            if (!Arrays.equals(myEncryptedMessage.getHash(), encryptedMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myEncryptedMessage.getJSONObject();
            response.put("verify", true);
            return response;
        }

        return JSON.emptyJSON;
    }

}
