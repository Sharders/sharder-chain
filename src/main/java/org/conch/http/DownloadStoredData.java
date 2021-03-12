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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.storage.Ssid;
import org.conch.storage.tx.StorageTxProcessorImpl;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static org.conch.http.JSONResponses.*;

public final class DownloadStoredData extends APIServlet.APIRequestHandler {

    static final DownloadStoredData instance = new DownloadStoredData();

    private DownloadStoredData() {
        super(new APITag[] {APITag.DATA_STORAGE}, "transaction","ssid","filename");
    }

    static private String ipfsAccessPort = Conch.getStringProperty("sharder.storage.ipfs.gateway.port");
    /**
     * Used to fetch the ssid details before download the stored object
     * @return
     */
    private JSONStreamAware fetchSsidInfo(HttpServletRequest request,HttpServletResponse response) throws IOException {
        String ssid = Convert.emptyToNull(request.getParameter("ssid"));
        String filename = Convert.emptyToNull(request.getParameter("filename"));
        if (StringUtils.isEmpty(ssid)) return null;
        String ipfsHashId = Ssid.decode(ssid);
        String ipfsUrl = "http://" + request.getServerName() + ":" + ipfsAccessPort + "/ipfs/"+ipfsHashId;
        //String res = null;
        // write the data into response
        String contentDisposition = "attachment";
        try {
            URI uri = new URI(null, null, filename, null);
            contentDisposition += "; filename*=UTF-8''" + uri.toASCIIString();
        } catch (URISyntaxException ignore) {

        }
        // write the data into response
        response.setHeader("Content-Disposition", contentDisposition);
        response.setContentType("application/octet-stream");
        OutputStream out = response.getOutputStream();
        long download = HttpUtil.download(ipfsUrl, out, true);
        System.out.println("file size: " + download);
       return null;
    }



    /**
     * Fetch the ss object and return the stream to response
     * @param request
     * @param response
     * @return
     * @throws ParameterException
     */
    private JSONStreamAware downloadSsObejctStream(HttpServletRequest request, HttpServletResponse response) throws ParameterException {
        String transactionIdString = Convert.emptyToNull(request.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }
        long transactionId = 0;
        Transaction transaction;
        try {
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Conch.getBlockchain().getTransaction(transactionId);
            } else {
                return UNKNOWN_TRANSACTION;
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }
        Attachment.DataStorageUpload attachment = (Attachment.DataStorageUpload) transaction.getAttachment();

        if (attachment == null) {
            return JSONResponses.incorrect("transaction", "stored data not found");
        }

        //Retrieve the data by ssid
        byte[] data;
        try {
            data = StorageTxProcessorImpl.getInstance().getData(transactionId);
        } catch (IOException e) {
            return JSONResponses.error("stored data not found");
        }
        if (!attachment.getType().equals("")) {
            response.setContentType(attachment.getType());
        } else {
            response.setContentType("application/octet-stream");
        }


        String filename = attachment.getName().trim();
        String contentDisposition = "attachment";
        try {
            URI uri = new URI(null, null, filename, null);
            contentDisposition += "; filename*=UTF-8''" + uri.toASCIIString();
        } catch (URISyntaxException ignore) {

        }

        // write the data into response
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

    /**
     * Redirect the download request to ipfs server.
     * e.g. localhost/downloadStoredData/ssid -> localhost:8088/ipfs/{ssid}
     * @return
     */
    private boolean redirectDownloadLink(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ssid = Convert.emptyToNull(request.getParameter("ssid"));
        if (StringUtils.isEmpty(ssid)) return false;

        String ipfsHashId = Ssid.decode(ssid);
        if (StringUtils.isEmpty(ipfsHashId)) return false;

        response.sendRedirect("http://" + request.getServerName() + ":" + ipfsAccessPort + "/ipfs/" + ipfsHashId);
        return true;
    }


    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws ConchException, IOException {
        JSONStreamAware ssidInfoResJson = fetchSsidInfo(request,response);
        if(ssidInfoResJson != null) return ssidInfoResJson;

        boolean redirectSuccess = false;
        try {
            redirectSuccess = redirectDownloadLink(request, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return redirectSuccess ? null : downloadSsObejctStream(request, response);
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        throw new UnsupportedOperationException();
    }

}
