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
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.http.biz.BizParameterRequestWrapper;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.conch.http.JSONResponses.*;
import static org.conch.util.Convert.*;
import static org.conch.util.JSON.JsonWrite;
import static org.conch.util.JSON.readJsonFile;
/**
 * @author bowen
 * @date 01/11/2020
 */
public final class Airdrop extends CreateTransaction {

    static final Airdrop instance = new Airdrop();

    public static class TransferInfo {
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

    static class DetectionTransferInfo extends TransferInfo {
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
     * default airdrop JSON fileName
     */
    public static final String DEFAULT_PATH_NAME = Constants.airdropJsonObj.getString("pathNameOfAirdrop");
    public static final String PATH_NAME_PREFIX_OF_AIRDROP_RESULT = Constants.airdropJsonObj.getString("pathNamePrefixOfAirdropResult");
    /**
     * list of valid keys used for validation
     */
    private static final JSONArray VALID_KEYS = Constants.airdropJsonObj.getJSONArray("validKeys");
    /**
     * airdrop switch
     */
    private static final boolean ENABLE_AIRDROP = Constants.airdropJsonObj.getBooleanValue("isEnable");
    /**
     * airdrop append Mode
     */
    private static final boolean IS_APPEND_MODE = Constants.airdropJsonObj.getBooleanValue("isAppendMode");

    /**
     * through the marked account, query the history of airdrops
     */
    public static final String markedAccount = Constants.airdropJsonObj.getString("markedAccount");

    private Airdrop() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "key", "jsonString", "isDetection");
    }

    private boolean verifyKey(String key) {
        for (Object validKey : VALID_KEYS) {
            if (validKey.toString().equalsIgnoreCase(key)) {
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
        String key = req.getParameter("key");
        String jsonString = req.getParameter("jsonString");
        boolean isDetection = "true".equalsIgnoreCase(req.getParameter("isDetection"));
        String airdropResultPathName = PATH_NAME_PREFIX_OF_AIRDROP_RESULT + new SimpleDateFormat(DATE_FORMAT).format(new Date()) + ".json";
        JSONObject parseObject;

        if (!ENABLE_AIRDROP) {
            return ACCESS_CLOSED;
        }
        if (jsonString != null) {
            if (!verifyKey(key)) {
                throw new ParameterException(incorrect("key", String.format("key %s is incorrect", key)));
            }
            // parse jsonString
            parseObject = JSON.parseObject(jsonString);
        } else {
            // parse file
            String jsonStr = readJsonFile(DEFAULT_PATH_NAME);
            parseObject = JSON.parseObject(jsonStr);
        }
        if (!isDetection) {
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
            // Check if the account balance meets the criteria
            if (!checkAccountBalance(list, parseObject.getString("secretPhrase"))) {
                return NOT_ENOUGH_FUNDS;
            }
            for (TransferInfo info : list) {
                org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
                try {
                    paramter.put("recipientRS", new String[]{info.getRecipientRS()});
                    paramter.put("recipientPublicKey", new String[]{info.getRecipientPublicKey()});
                    paramter.put("amountNQT", new String[]{getAmountByAirdropRate(info.getAmountNQT())});
                    paramter.put("transactionID", new String[]{info.getTransactionID()});
                    paramter.put("errorDescription", new String[]{info.getErrorDescription()});

                    BizParameterRequestWrapper reqWrapper = new BizParameterRequestWrapper(req, Maps.newHashMap(), paramter);
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
            airdropRate = 1;
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
                    JsonWrite(jsonObject, airdropResultPathName);
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
        } else {

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
                    jsonObject.put("errorResponse", JSONValue.parse(org.conch.util.JSON.toString(MISSING_TRANSACTION)));
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
                    JsonWrite(jsonObject, airdropResultPathName);
                } catch (Exception e) {
                    response.put("writeToFileError", e.getMessage());
                }
            }

            response.put("detectionCount", detectionResponse.size());
            response.put("detectionResponse", detectionResponse);

            return response;
        }
    }

    private boolean checkAccountBalance(List<TransferInfo> list, String secretPhrase) {
        long totalFunds = 0L;
        for (TransferInfo info : list) {
            long parseLong = Long.parseLong(info.getAmountNQT());
            totalFunds += parseLong;
        }
        long accountId = Account.getId(secretPhrase);
        Account account = Account.getAccount(accountId);
        long accountEffectiveBalanceNQT = account.getEffectiveBalanceNQT(Conch.getHeight());
        if (accountEffectiveBalanceNQT > totalFunds + list.size() * Constants.ONE_SS) {
            return true;
        }
        return false;
    }

    private String getAmountByAirdropRate(String amountNQT) {
        if (airdropRate != 1) {
            Long newAmountNQT = Long.parseLong(amountNQT) * airdropRate;
            return newAmountNQT.toString();
        }
        return amountNQT;
    }

    private static long airdropRate = 1;

    // auto airdrop
    public static final Runnable autoAirdropThread = new Runnable() {
        @Override
        public void run() {
            try {
                if (!Conch.getBlockchainProcessor().isUpToDate()) {
                    Logger.logInfoMessage("The current blockchain state is not up to date");
                    return;
                }
                // parse file
                String jsonStr = readJsonFile(DEFAULT_PATH_NAME);
                JSONObject parseObject = JSON.parseObject(jsonStr);
                String secretPhrase = parseObject.getString("secretPhrase");
                if (secretPhrase == null) {
                    Logger.logInfoMessage("The auto airdrop secretPhrase is missing");
                    return;
                }
                DbIterator<? extends Transaction> iterator = null;
                org.json.simple.JSONArray transactions = new org.json.simple.JSONArray();
                int from = 0;
                int to = 0;

                try {
                    iterator = Conch.getBlockchain().getTransactions(Account.getId(secretPhrase), Account.rsAccountToId(markedAccount), TransactionType.TYPE_PAYMENT,from, to);
                    while (iterator.hasNext()) {
                        Transaction transaction = iterator.next();
                        transactions.add(JSONData.transaction(transaction, false));
                    }
                }finally {
                    DbUtils.close(iterator);
                }
                if (transactions.size() == 0) {
                    Logger.logInfoMessage("No relevant transaction records were found");
                    return;
                }
                Integer timestamp = 0;
                for (Object transaction : transactions) {
                    org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) transaction;
                    Integer timestampNext =(Integer) jsonObject.get("timestamp");
                    timestamp = timestampNext;
                }

                String txDateFrom = DateFormatUtils.format(new Date(fromEpochTime(timestamp)), DATE_FORMAT);
                String currentDate = new SimpleDateFormat(DATE_FORMAT).format(new Date());
                // if today is airdropped, skip
                if (!currentDate.equals(txDateFrom)) {
                    airdropRate = (DateUtils.parseDate(currentDate, DATE_FORMAT).getTime() - DateUtils.parseDate(txDateFrom, DATE_FORMAT).getTime()) / TimeUnit.DAYS.toMillis(1);
                    Airdrop.instance.processRequest(new HttpServletRequest() {
                            @Override
                            public String getAuthType() {
                                return null;
                            }

                            @Override
                            public Cookie[] getCookies() {
                                return new Cookie[0];
                            }

                            @Override
                            public long getDateHeader(String s) {
                                return 0;
                            }

                            @Override
                            public String getHeader(String s) {
                                return null;
                            }

                            @Override
                            public Enumeration<String> getHeaders(String s) {
                                return null;
                            }

                            @Override
                            public Enumeration<String> getHeaderNames() {
                                return null;
                            }

                            @Override
                            public int getIntHeader(String s) {
                                return 0;
                            }

                            @Override
                            public String getMethod() {
                                return null;
                            }

                            @Override
                            public String getPathInfo() {
                                return null;
                            }

                            @Override
                            public String getPathTranslated() {
                                return null;
                            }

                            @Override
                            public String getContextPath() {
                                return null;
                            }

                            @Override
                            public String getQueryString() {
                                return null;
                            }

                            @Override
                            public String getRemoteUser() {
                                return null;
                            }

                            @Override
                            public boolean isUserInRole(String s) {
                                return false;
                            }

                            @Override
                            public Principal getUserPrincipal() {
                                return null;
                            }

                            @Override
                            public String getRequestedSessionId() {
                                return null;
                            }

                            @Override
                            public String getRequestURI() {
                                return null;
                            }

                            @Override
                            public StringBuffer getRequestURL() {
                                return null;
                            }

                            @Override
                            public String getServletPath() {
                                return null;
                            }

                            @Override
                            public HttpSession getSession(boolean b) {
                                return null;
                            }

                            @Override
                            public HttpSession getSession() {
                                return null;
                            }

                            @Override
                            public String changeSessionId() {
                                return null;
                            }

                            @Override
                            public boolean isRequestedSessionIdValid() {
                                return false;
                            }

                            @Override
                            public boolean isRequestedSessionIdFromCookie() {
                                return false;
                            }

                            @Override
                            public boolean isRequestedSessionIdFromURL() {
                                return false;
                            }

                            @Override
                            public boolean isRequestedSessionIdFromUrl() {
                                return false;
                            }

                            @Override
                            public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
                                return false;
                            }

                            @Override
                            public void login(String s, String s1) throws ServletException {

                            }

                            @Override
                            public void logout() throws ServletException {

                            }

                            @Override
                            public Collection<Part> getParts() throws IOException, ServletException {
                                return null;
                            }

                            @Override
                            public Part getPart(String s) throws IOException, ServletException {
                                return null;
                            }

                            @Override
                            public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
                                return null;
                            }

                            @Override
                            public Object getAttribute(String s) {
                                return null;
                            }

                            @Override
                            public Enumeration<String> getAttributeNames() {
                                return null;
                            }

                            @Override
                            public String getCharacterEncoding() {
                                return null;
                            }

                            @Override
                            public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

                            }

                            @Override
                            public int getContentLength() {
                                return 0;
                            }

                            @Override
                            public long getContentLengthLong() {
                                return 0;
                            }

                            @Override
                            public String getContentType() {
                                return null;
                            }

                            @Override
                            public ServletInputStream getInputStream() throws IOException {
                                return null;
                            }

                            @Override
                            public String getParameter(String s) {
                                return null;
                            }

                            @Override
                            public Enumeration<String> getParameterNames() {
                                return null;
                            }

                            @Override
                            public String[] getParameterValues(String s) {
                                return new String[0];
                            }

                            @Override
                            public Map<String, String[]> getParameterMap() {
                                return null;
                            }

                            @Override
                            public String getProtocol() {
                                return null;
                            }

                            @Override
                            public String getScheme() {
                                return null;
                            }

                            @Override
                            public String getServerName() {
                                return null;
                            }

                            @Override
                            public int getServerPort() {
                                return 0;
                            }

                            @Override
                            public BufferedReader getReader() throws IOException {
                                return null;
                            }

                            @Override
                            public String getRemoteAddr() {
                                return null;
                            }

                            @Override
                            public String getRemoteHost() {
                                return null;
                            }

                            @Override
                            public void setAttribute(String s, Object o) {

                            }

                            @Override
                            public void removeAttribute(String s) {

                            }

                            @Override
                            public Locale getLocale() {
                                return null;
                            }

                            @Override
                            public Enumeration<Locale> getLocales() {
                                return null;
                            }

                            @Override
                            public boolean isSecure() {
                                return false;
                            }

                            @Override
                            public RequestDispatcher getRequestDispatcher(String s) {
                                return null;
                            }

                            @Override
                            public String getRealPath(String s) {
                                return null;
                            }

                            @Override
                            public int getRemotePort() {
                                return 0;
                            }

                            @Override
                            public String getLocalName() {
                                return null;
                            }

                            @Override
                            public String getLocalAddr() {
                                return null;
                            }

                            @Override
                            public int getLocalPort() {
                                return 0;
                            }

                            @Override
                            public ServletContext getServletContext() {
                                return null;
                            }

                            @Override
                            public AsyncContext startAsync() throws IllegalStateException {
                                return null;
                            }

                            @Override
                            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
                                return null;
                            }

                            @Override
                            public boolean isAsyncStarted() {
                                return false;
                            }

                            @Override
                            public boolean isAsyncSupported() {
                                return false;
                            }

                            @Override
                            public AsyncContext getAsyncContext() {
                                return null;
                            }

                            @Override
                            public DispatcherType getDispatcherType() {
                                return null;
                            }
                        });
                }
            } catch (Exception e) {
//                e.printStackTrace();
                Logger.logInfoMessage("Open auto-airdrop failed");
            }
        }
    };
}
