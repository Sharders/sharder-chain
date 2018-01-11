/*
 * Copyright © 2017 sharder.org.
 * Copyright © 2014-2017 ichaoj.com.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,
 * no part of the COS software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package org.conch.http;

import org.conch.ConchException;
import org.conch.Transaction;
import org.conch.crypto.Crypto;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;

import static org.conch.http.JSONResponses.MISSING_SIGNATURE_HASH;

public final class CalculateFullHash extends APIServlet.APIRequestHandler {

    static final CalculateFullHash instance = new CalculateFullHash();

    private CalculateFullHash() {
        super(new APITag[] {APITag.TRANSACTIONS}, "unsignedTransactionBytes", "unsignedTransactionJSON", "signatureHash");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String unsignedBytesString = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String signatureHashString = Convert.emptyToNull(req.getParameter("signatureHash"));
        String unsignedTransactionJSONString = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));

        if (signatureHashString == null) {
            return MISSING_SIGNATURE_HASH;
        }
        JSONObject response = new JSONObject();
        try {
            Transaction transaction = ParameterParser.parseTransaction(unsignedTransactionJSONString, unsignedBytesString, null).build();
            MessageDigest digest = Crypto.sha256();
            digest.update(transaction.getUnsignedBytes());
            byte[] fullHash = digest.digest(Convert.parseHexString(signatureHashString));
            response.put("fullHash", Convert.toHexString(fullHash));
        } catch (ConchException.NotValidException e) {
            JSONData.putException(response, e, "Incorrect unsigned transaction json or bytes");
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
