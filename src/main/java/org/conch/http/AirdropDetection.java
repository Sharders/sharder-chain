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
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.http.biz.BizParameterRequestWrapper;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.conch.http.JSONResponses.*;
import static org.conch.util.JSON.JsonWrite;
import static org.conch.util.JSON.readJsonFile;

public final class AirdropDetection extends CreateTransaction {

    static final AirdropDetection instance = new AirdropDetection();

    static class DetectionTransferInfo extends Airdrop.TransferInfo {
        private String blockId;
        private Integer blockHeight;
        private Integer confirmCount;

        DetectionTransferInfo(Airdrop.TransferInfo transferInfo) {
            this.setTransactionID(transferInfo.getTransactionID());
            this.setRecipientPublicKey(transferInfo.getRecipientPublicKey());
            this.setRecipientRS(transferInfo.getRecipientRS());
            this.setAmountNQT(transferInfo.getAmountNQT());
        }

        public String getBlockId() {
            return blockId;
        }

        public void setBlockId(String blockId) {
            this.blockId = blockId;
        }

        public Integer getBlockHeight() {
            return blockHeight;
        }

        public void setBlockHeight(Integer blockHeight) {
            this.blockHeight = blockHeight;
        }

        public Integer getConfirmCount() {
            return confirmCount;
        }

        public void setConfirmCount(Integer confirmCount) {
            this.confirmCount = confirmCount;
        }
    }

    /**
     *  default airdrop JSON fileName
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

    private AirdropDetection() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "pathAndFileName", "key", "jsonString");
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
        
        JSONArray doneListOrigin = parseObject.getJSONArray("doneList");
        List<Airdrop.TransferInfo> doneList = doneListOrigin == null ? new ArrayList<>() : JSONObject.parseArray(doneListOrigin.toJSONString(), Airdrop.TransferInfo.class);
        if (doneList.isEmpty()) {
            return MISSING_TRANSACTION;
        }
        JSONArray detectionResponse = new JSONArray();
        JSONArray confirmedList = new JSONArray();
        ArrayList<Airdrop.TransferInfo> doneListAfter = new ArrayList<>();
        for (Airdrop.TransferInfo info : doneList) {
            DetectionTransferInfo detectionTransferInfo = new DetectionTransferInfo(info);
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            Map<String, String[]> paramter = Maps.newHashMap();
            paramter.put("transactionID", new String[]{info.getTransactionID()});
            jsonObject.put("transactionID", info.getTransactionID());
            BizParameterRequestWrapper reqWrapper = new BizParameterRequestWrapper(req, req.getParameterMap(), paramter);

            String transactionIdString = Convert.emptyToNull(reqWrapper.getParameter("transactionID"));
            if (transactionIdString == null) {
                jsonObject.put("errorResponse", MISSING_TRANSACTION);
            }
            boolean includePhasingResult = "true".equalsIgnoreCase(reqWrapper.getParameter("includePhasingResult"));

            long transactionId = 0;
            Transaction transaction = null;
            try {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Conch.getBlockchain().getTransaction(transactionId);
            } catch (RuntimeException e) {
                jsonObject.put("errorResponse", JSONValue.parse(org.conch.util.JSON.toString(INCORRECT_TRANSACTION)));
            }

            if (transaction == null) {
                transaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
                if (transaction == null) {
                    jsonObject.put("errorResponse", JSONValue.parse(org.conch.util.JSON.toString(UNKNOWN_TRANSACTION)));
                } else {
                    jsonObject.put("unconfirmedTransaction", JSONData.unconfirmedTransaction(transaction));
                }
                doneListAfter.add(info);
            } else {
                org.json.simple.JSONObject transactionJson = JSONData.transaction(transaction, includePhasingResult);
                jsonObject.put("confirmedTransaction", transactionJson);
                // Add relevant information to detectionTransferInfo class
                detectionTransferInfo.setBlockId((String) transactionJson.get("block"));
                detectionTransferInfo.setBlockHeight((Integer) transactionJson.get("height"));
                detectionTransferInfo.setConfirmCount((Integer) transactionJson.get("confirmations"));
                confirmedList.add(JSON.toJSON(detectionTransferInfo));
            }
            detectionResponse.add(jsonObject);
        }

        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();

        jsonObject.put("secretPhrase", parseObject.getString("secretPhrase"));
        jsonObject.put("feeNQT", parseObject.getString("feeNQT"));
        jsonObject.put("deadline", parseObject.getString("deadline"));

        jsonObject.put("doneList", JSON.toJSON(doneListAfter));
        jsonObject.put("failList", parseObject.getJSONArray("failList"));
        jsonObject.put("list", parseObject.getJSONArray("list"));
        jsonObject.put("confirmedList", JSON.toJSON(confirmedList));

        if (StringUtils.isNotEmpty(jsonString)) {
            response.put("jsonResult", jsonObject);
        } else {
            try {
                JsonWrite(jsonObject, pathName);
            } catch (Exception e) {
                response.put("writeToFileError", e.getMessage());
            }
        }

        response.put("detectionCount", detectionResponse.size());
        response.put("detectionResponse", detectionResponse);

        return response;
    }
}
