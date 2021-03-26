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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.http.biz.BizParameterRequestWrapper;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.conch.http.JSONResponses.*;
import static org.conch.util.JSON.JsonWrite;
import static org.conch.util.JSON.readJsonFile;

public final class Airdrop extends CreateTransaction {

    static final Airdrop instance = new Airdrop();

    static class TransferInfo {
        private String recipientRS;
        private String amountNQT;
        private String recipientPublicKey;
        private String errorDescription; // create transaction failed to write to this value
        private String transactionID; // create transaction succeed to write to this value

        TransferInfo() {
        }

        public String getRecipientRS() {
            return recipientRS;
        }

        public void setRecipientRS(String recipientRS) {
            this.recipientRS = recipientRS;
        }

        public String getAmountNQT() {
            return amountNQT;
        }

        public void setAmountNQT(String amountNQT) {
            this.amountNQT = amountNQT;
        }

        public String getRecipientPublicKey() {
            return recipientPublicKey;
        }

        public void setRecipientPublicKey(String recipientPublicKey) {
            this.recipientPublicKey = recipientPublicKey;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public void setErrorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
        }

        public String getTransactionID() {
            return transactionID;
        }

        public void setTransactionID(String transactionID) {
            this.transactionID = transactionID;
        }
    }

    /**
     * default airdrop JSON fileName
     */
    private static final String DEFAULT_PATH_NAME = Conch.getStringProperty("sharder.airdrop.pathName");
    /**
     * list of valid keys used for validation
     */
    private static final List<String> VALID_KEYS = Conch.getStringListProperty("sharder.airdrop.validKeys");
    /**
     * airdrop switch
     */
    private static final boolean ENABLE_AIRDROP = Conch.getBooleanProperty("sharder.airdrop.enable");
    /**
     * airdrop append Mode
     */
    private static final boolean IS_APPEND_MODE = Conch.getBooleanProperty("sharder.airdrop.isAppendMode");

    private Airdrop() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "pathName", "key", "jsonString");
    }

    private boolean verifyKey(String key) {
        for (String validKey : VALID_KEYS) {
            if (validKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        // If the height is not empty, return
        if (Conch.getAirdropHeighStatus(Conch.getBlockchain().getHeight()) == true) {
            return ACCESS_CLOSED_AT_HEIGHT;
        }
        if (Constants.isLightClient) {
            throw new ParameterException(NOT_ENABLE_ON_LIGHTCLIENT);
        }
        org.json.simple.JSONObject response = new org.json.simple.JSONObject();
        String pathName = req.getParameter("pathName");
        String key = req.getParameter("key");
        String jsonString = req.getParameter("jsonString");
        if (!ENABLE_AIRDROP) {
            return ACCESS_CLOSED;
        }
        if (!verifyKey(key)) {
            throw new ParameterException(incorrect("key", String.format("key %s is incorrect", key)));
        }

        JSONObject parseObject;

        if (jsonString != null) {
            // parse jsonString
            parseObject = JSON.parseObject(jsonString);
        } else {
            // parse file
            pathName = pathName == null ? DEFAULT_PATH_NAME : pathName;
            String jsonStr = readJsonFile(pathName);
            parseObject = JSON.parseObject(jsonStr);
        }

        Map<String, String[]> paramter = Maps.newHashMap();
        paramter.put("secretPhrase", new String[]{parseObject.getString("secretPhrase")});
        paramter.put("feeNQT", new String[]{parseObject.getString("feeNQT")});
        paramter.put("deadline", new String[]{parseObject.getString("deadline")});

        JSONArray listOrigin = parseObject.getJSONArray("list");
        JSONArray doneListOrigin = parseObject.getJSONArray("doneList");
        JSONArray failListOrigin = parseObject.getJSONArray("failList");
        if (listOrigin == null) {
            return MISSING_TRANSACTION;
        }
        if (!IS_APPEND_MODE) {
            doneListOrigin = null;
            failListOrigin = null;
        }
        List<TransferInfo> list = JSONObject.parseArray(listOrigin.toJSONString(), TransferInfo.class);
        // record existing lists, append pattern
        List<TransferInfo> doneList = doneListOrigin == null ? new ArrayList<>() : JSONObject.parseArray(doneListOrigin.toJSONString(), TransferInfo.class);
        List<TransferInfo> failList = failListOrigin == null ? new ArrayList<>() : JSONObject.parseArray(failListOrigin.toJSONString(), TransferInfo.class);
        // record the lists that unhandled the exception
        List<TransferInfo> pendingList = new ArrayList<>();

        JSONArray transferSuccessList = new JSONArray();
        JSONArray transferFailList = new JSONArray();
        for (TransferInfo info : list) {
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            try {
                paramter.put("recipientRS", new String[]{info.getRecipientRS()});
                paramter.put("recipientPublicKey", new String[]{info.getRecipientPublicKey()});
                paramter.put("amountNQT", new String[]{info.getAmountNQT()});
                paramter.put("transactionID", new String[]{info.getTransactionID()});
                paramter.put("errorDescription", new String[]{info.getErrorDescription()});

                BizParameterRequestWrapper reqWrapper = new BizParameterRequestWrapper(req, req.getParameterMap(), paramter);
                Account account = ParameterParser.getSenderAccount(reqWrapper);

                long recipient = ParameterParser.getAccountId(reqWrapper, "recipientRS", true);
                long amountNQT = ParameterParser.getAmountNQT(reqWrapper);

                JSONStreamAware transaction = createTransaction(reqWrapper, account, recipient, amountNQT);
                org.json.simple.JSONObject transactionJsonObject = (org.json.simple.JSONObject) JSONValue.parse(org.conch.util.JSON.toString(transaction));
                if (transactionJsonObject.get("broadcasted") != null && transactionJsonObject.get("broadcasted").equals(true)) {
                    // write info to the doneList
                    info.setTransactionID((String) transactionJsonObject.get("transaction"));
                    doneList.add(info);
                    jsonObject.put("transactionInfo", transaction);
                    jsonObject.put("transferInfo", JSON.toJSON(info));
                    // transaction was created successfully and broadcast
                    transferSuccessList.add(jsonObject);
                } else {
                    // write info to failList
                    info.setErrorDescription((String) transactionJsonObject.get("errorDescription"));
                    failList.add(info);
                    transferFailList.add(JSON.toJSON(info));
                }

            } catch (ParameterException e) {
                e.printStackTrace();

                org.json.simple.JSONObject errorResponse = (org.json.simple.JSONObject) JSONValue.parse(org.conch.util.JSON.toString(e.getErrorResponse()));
                info.setErrorDescription((String) errorResponse.get("errorDescription"));
                failList.add(info);
                transferFailList.add(JSON.toJSON(info));
            } catch (ConchException e) {
                e.printStackTrace();

                info.setErrorDescription(e.getMessage());
                pendingList.add(info);
                transferFailList.add(JSON.toJSON(info));
            } catch (Exception e) {
                // catch all exception, ensure that processing does not break
                e.printStackTrace();

                info.setErrorDescription(e.getMessage());
                pendingList.add(info);
                transferFailList.add(JSON.toJSON(info));


            }
        }
        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();

        jsonObject.put("secretPhrase", parseObject.getString("secretPhrase"));
        jsonObject.put("feeNQT", parseObject.getString("feeNQT"));
        jsonObject.put("deadline", parseObject.getString("deadline"));

        jsonObject.put("doneList", JSON.toJSON(doneList));
        jsonObject.put("failList", JSON.toJSON(failList));
        jsonObject.put("list", JSON.toJSON(pendingList));

        if (jsonString != null) {
            response.put("jsonResult", jsonObject);
        } else {
            try {
                JsonWrite(jsonObject, pathName);
            } catch (Exception e) {
                response.put("writeToFileError", e.getMessage());
            }
        }

        response.put("transferSuccessList", transferSuccessList);
        response.put("transferFailList", transferFailList);
        response.put("transferTotalCount", list.size());
        response.put("transferSuccessCount", transferSuccessList.size());

        // Set this height to no longer be airdropped
        Conch.setAirdropHeighStatus(Conch.getBlockchain().getHeight(), false);

        return response;
    }
}
