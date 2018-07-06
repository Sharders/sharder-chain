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
import org.conch.ConchException;
import org.conch.Storage;
import org.conch.TaggedData;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.conch.http.JSONResponses.PRUNED_TRANSACTION;

public final class DownloadStoredData extends APIServlet.APIRequestHandler {

    static final DownloadStoredData instance = new DownloadStoredData();

    private DownloadStoredData() {
        super(new APITag[] {APITag.DATA_STORAGE}, "transaction");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws ConchException {
        long transactionId = ParameterParser.getUnsignedLong(request, "transaction", true);
        Storage storage = Storage.getDataStorage(transactionId);

        if (storage == null) {
            return JSONResponses.incorrect("transaction", "stored data not found");
        }
        byte[] data;
        try {
            data = Storage.getData(transactionId);
        } catch (IOException e) {
            return JSONResponses.error("stored data not found");
        }
        if (!storage.getType().equals("")) {
            response.setContentType(storage.getType());
        } else {
            response.setContentType("application/octet-stream");
        }
        String filename = storage.getName().trim();
        String contentDisposition = "attachment";
        try {
            URI uri = new URI(null, null, filename, null);
            contentDisposition += "; filename*=UTF-8''" + uri.toASCIIString();
        } catch (URISyntaxException ignore) {}
        response.setHeader("Content-Disposition", contentDisposition);
        response.setContentLength(data.length);
        try (OutputStream out = response.getOutputStream()) {
            try {
                out.write(data);
            } catch (IOException e) {
                throw new ParameterException(JSONResponses.RESPONSE_WRITE_ERROR);
            }
        } catch (IOException e) {
            throw new ParameterException(JSONResponses.RESPONSE_STREAM_ERROR);
        }
        return null;
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        throw new UnsupportedOperationException();
    }
}
