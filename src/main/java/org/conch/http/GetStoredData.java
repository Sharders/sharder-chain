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

import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.tx.Transaction;
import org.conch.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class GetStoredData extends APIServlet.APIRequestHandler {

    static final GetStoredData instance = new GetStoredData();

    private GetStoredData() {
        super(new APITag[] {APITag.DATA_STORAGE}, "transaction", "includeData");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req,HttpServletResponse response) throws ConchException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        boolean includeData = !"false".equalsIgnoreCase(req.getParameter("includeData"));

        Transaction storeTransaction = Conch.getBlockchain().getTransaction(transactionId);
        try {
            return JSONData.storedData(storeTransaction, includeData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JSON.emptyJSON;
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        throw new UnsupportedOperationException();
    }

}
