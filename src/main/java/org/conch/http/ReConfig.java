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
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.mq.Message;
import org.conch.mq.MessageManager;
import org.conch.peer.Peer;
import org.conch.tools.ClientUpgradeTool;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * @author jiangbubai
 * @date  2019-05-09 updated by Ben
 */
public final class ReConfig extends APIServlet.APIRequestHandler {

    static final ReConfig INSTANCE = new ReConfig();
    private static final List<String> EXCLUDE_PARAMS = Arrays.asList(
            "restart", "requestType", "newAdminPassword", "isInit", "registerStatus",
            "adminPassword", "reBind", "username", "password", "nodeType", "sharderAccount");
    private static final String URL = UrlManager.getFoundationUrl(
            UrlManager.HUB_SETTING_ACCOUNT_CHECK_EOLINKER,
            UrlManager.HUB_SETTING_ACCOUNT_CHECK_LOCAL,
            UrlManager.HUB_SETTING_ACCOUNT_CHECK_PATH
    );

    private static final String SF_BIND_URL = UrlManager.getFoundationUrl(
            "",
            UrlManager.HUB_SETTING_ADDRESS_BIND_LOCAL,
            UrlManager.HUB_SETTING_ADDRESS_BIND_PATH
    );

    private ReConfig() {
        super(new APITag[] {APITag.DEBUG}, "restart");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        boolean restart = "true".equalsIgnoreCase(req.getParameter("restart"));
        boolean bindNew = "true".equalsIgnoreCase(req.getParameter("reBind"));
        boolean isInit = "true".equalsIgnoreCase(req.getParameter("isInit"));
        HashMap map = new HashMap(16);
        Enumeration enu = req.getParameterNames();

//        Conch.getStringProperty("sharder.HubBindAddress");

        String accountPR = Conch.getStringProperty("sharder.HubBindPassPhrase");
        String inputLinkedPR = req.getParameter("sharder.HubBindPassPhrase");
        if(StringUtils.isNotEmpty(inputLinkedPR)) {
            accountPR = inputLinkedPR;
        }
        long creatorId = Account.getId(accountPR);
        String bindRs = Account.rsAccount(creatorId);


        if (SharderPoolProcessor.whetherCreatorHasWorkingMinePool(creatorId)) {
            response.put("reconfiged", false);
            response.put("failedReason", "Account " +  bindRs +" has created a pool already");
            return response;
        }

//        CertifiedPeer linkedPeer = Conch.getPocProcessor().getLinkedPeer(creatorId);
//        String myAddress = Convert.nullToEmpty(req.getParameter("sharder.myAddress"));
//        boolean samePeer = linkedPeer != null && linkedPeer.getHost() != null && (linkedPeer.getHost().equals(myAddress));
//        if(linkedPeer != null && !samePeer) {
//            response.put("reconfiged", false);
//            response.put("failedReason", "Account " + bindRs +" is already linked to a hub");
//            return response;
//        }

//        if (!verifyFormData(req, response)) {
//            Logger.logErrorMessage("failed to configure settings caused by formData invalid!");
//            response.put("reconfiged", false);
//            response.put("failedReason", "Failed to configure settings caused by input values invalid!");
//            return response;
//        }

        // send the address binding request to foundation
        try {
            sendAddrBindingAndTypeTxCreationRequestToFoundation(req, bindRs);
        } catch (ConchException.NotValidException e) {
            Logger.logErrorMessage("failed to configure settings caused by update linked address to foundation failed[" + e.getMessage() + "]");
            response.put("reconfiged", false);
            response.put("failedReason", "Failed to configure settings caused by [" + e.getMessage() + "]");
            return response;
        }

//        // send to foundation to validate and create a node type tx
//        if (!sendCreateNodeTypeTxRequestToFoundation(req, bindRs)) {
//            Logger.logErrorMessage("failed to configure settings caused by send create node type tx message to foundation failed!");
//            response.put("reconfiged", false);
//            response.put("failedReason", "Failed to configure settings caused by node type tx creation failed!");
//            return response;
//        }

        while(enu.hasMoreElements()) {
            String paraName = (String)enu.nextElement();

            if ("sharder.HubBindPassPhrase".equals(paraName)) {
                String prFromRequest = req.getParameter(paraName);
                map.put("sharder.HubBindPassPhrase", prFromRequest);
                map.put("sharder.HubBindAddress", bindRs);
                continue;
            }
            if ("newAdminPassword".equals(paraName)) {
                map.put("sharder.adminPassword", req.getParameter(paraName));
                continue;
            }
            if ("sharderAccount".equals(paraName)) {
                //set the siteAccount into properties after bind operation success
                map.put("sharder.siteAccount", req.getParameter("sharderAccount"));
                continue;
            }
//            //if it ins't initial, use the current pr get from local properties file
//            if ("sharder.HubBindPassPhrase".equals(paraName) && needBind && !bindNew && !isInit) {
//                map.put("sharder.HubBindPassPhrase", Conch.getStringProperty("sharder.HubBindPassPhrase"));
//                continue;
//            }

            // pre-defined exclusion request parameter
            if (EXCLUDE_PARAMS.contains(paraName)) {
                continue;
            }
            map.put(paraName, req.getParameter(paraName));
        }

        Conch.storePropertiesToFile(map);

        if (restart) {
            new Thread(() -> {
                // get the default db file
                if(isInit && Constants.initFromArchivedDbFile) {
                    Logger.logDebugMessage("Fetch and upgrade the default archived db file to local in the Hub initialization phase");
                    ClientUpgradeTool.restoreDbToLastArchive();
                }

                Conch.restartApplication(null);
            }).start();
        }
        response.put("reconfiged", true);
        return response;
    }

    /**
     * Check whether the data is correct
     * @param req HttpServletRequest
     * @param response json object
     * @return true:correct data; false:wrong data
     */
    private Boolean verifyFormData(HttpServletRequest req, JSONObject response) {
        boolean result = true;
        boolean useNATService = Boolean.TRUE.toString().equalsIgnoreCase(req.getParameter("sharder.useNATService"));
        String nodeType = Convert.nullToEmpty(req.getParameter("nodeType"));
        Map<String, String> pageValues = new HashMap<>(16);

        if (Peer.Type.NORMAL.matchSimpleName(nodeType)) {
            pageValues.put("status", Convert.nullToEmpty(req.getParameter("registerStatus")));
        }
        pageValues.put("natServiceIp", Convert.nullToEmpty(req.getParameter("sharder.NATServiceAddress")));
        pageValues.put("natServicePort", Convert.nullToEmpty(req.getParameter("sharder.NATServicePort")));
        pageValues.put("natClientKey", Convert.nullToEmpty(req.getParameter("sharder.NATClientKey")));
        pageValues.put("proxyAddress", Convert.nullToEmpty(req.getParameter("sharder.myAddress")));
        pageValues.put("type", nodeType);
        Iterator<Map.Entry<String, String>> iterator = pageValues.entrySet().iterator();

        if (useNATService) {
            try {
                RestfulHttpClient.HttpResponse verifyResponse = RestfulHttpClient.getClient(URL)
                        .post()
                        .addPostParam("sharderAccount", req.getParameter("sharderAccount"))
                        .addPostParam("password", req.getParameter("password"))
                        .addPostParam("nodeType", req.getParameter("nodeType"))
                        .addPostParam("serialNum", Conch.getSerialNum())
                        .request();
                boolean querySuccess = com.alibaba.fastjson.JSONObject.parseObject(verifyResponse.getContent()).getBooleanValue(Constants.SUCCESS);
                if (querySuccess) {
                    com.alibaba.fastjson.JSONObject hubSetting = (com.alibaba.fastjson.JSONObject) JSON.toJSON(
                            JSON.parseObject(verifyResponse.getContent()).get(Constants.DATA)
                    );
                    while (iterator.hasNext()) {
                        Map.Entry<String, String> pageValue = iterator.next();
                        String dbValue;
                        Object value = pageValue.getValue();
                        if ("type".equals(pageValue.getKey())) {
                            dbValue = Peer.Type.getSimpleName(hubSetting.getInteger(pageValue.getKey()));
                        } else {
                            dbValue = hubSetting.getString(pageValue.getKey());
                        }
                        if (!this.doVerify(value, dbValue)) {
                            result = false;
                            response.put("reconfiged", false);
                            response.put("failedReason", "Tampered data detected! failed to reconfigure!");
                            break;
                        }
                    }
                } else {
                    result = false;
                    response.put("reconfiged", false);
                    response.put("failedReason", "Tampered data detected! failed to reconfigure!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                response.put("reconfiged", false);
                response.put("failedReason", "Connection error");
            }
        }

        return result;
    }

    private Boolean doVerify(Object pageValue, Object dbValue) {
        boolean bothNull = pageValue == null && dbValue == null;
        boolean unilateralNull = (pageValue == null && dbValue != null) || (pageValue != null && dbValue == null);
        if (bothNull) {
            return true;
        } else if (unilateralNull) {
            return false;
        } else {
            return pageValue.equals(dbValue);
        }
    }

    /**
     * send request to foundation to :
     * 1. bind addr to the machine
     * 2. create a node type tx to certify the node
     * @param req
     * @param rsAddress
     * @return
     */
    private void sendAddrBindingAndTypeTxCreationRequestToFoundation(HttpServletRequest req, String rsAddress) throws ConchException.NotValidException {
        RestfulHttpClient.HttpResponse verifyResponse = null;
        try {
            String myAddress = Convert.nullToEmpty(req.getParameter("sharder.myAddress"));
            if(Conch.systemInfo == null) GetNodeHardware.readSystemInfo();
            RestfulHttpClient.HttpClient client = RestfulHttpClient.getClient(SF_BIND_URL)
                    .post()
                    .addPostParam("sharderAccount", req.getParameter("sharderAccount"))
                    .addPostParam("password", req.getParameter("password"))
                    .addPostParam("ip", myAddress)
                    .addPostParam("network", Conch.getNetworkType())
                    .addPostParam("nodeType", req.getParameter("nodeType"))
                    .addPostParam("serialNum", Conch.getSerialNum())
                    .addPostParam("tssAddress", rsAddress)
                    .addPostParam("diskCapacity", String.valueOf(Conch.systemInfo.getHardDiskSize()))
                    .addPostParam("from", "NodeInitialStage#Reconfig");

            Logger.logInfoMessage("send binding and NodeTypeTx creation request to foundation " + SF_BIND_URL + ": " + client.getPostParams());

            verifyResponse = client.request();
            com.alibaba.fastjson.JSONObject responseObj = com.alibaba.fastjson.JSONObject.parseObject(verifyResponse.getContent());
            if(!responseObj.getBooleanValue(Constants.SUCCESS)) {
                throw new ConchException.NotValidException(responseObj.getString("data"));
            }
        }  catch (IOException e) {
            Logger.logErrorMessage("[ ERROR ]Failed to update linked address to foundation.", e);
            throw new ConchException.NotValidException(e.getMessage());
        }
    }

    //TODO need refactor
    @SuppressWarnings("unchecked")
    private Boolean sendCreateNodeTypeTxRequestToFoundation(HttpServletRequest req, String rsAddress) {
        boolean result = false;
        String myAddress = Convert.nullToEmpty(req.getParameter("sharder.myAddress"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ip", myAddress);
        jsonObject.put("type", req.getParameter("nodeType"));
        jsonObject.put("network", Conch.getNetworkType());
        jsonObject.put("diskCapacity", GetNodeHardware.diskCapacity(GetNodeHardware.DISK_UNIT_TYPE_KB));
        jsonObject.put("bindRs", rsAddress);
        jsonObject.put("from", "NodeInitialStage#Reconfig");
        Message message = new Message()
                .setSender(myAddress)
                .setRetryCount(0)
                .setTimestamp(System.currentTimeMillis())
                .setType(Message.Type.NODE_TYPE.getName())
                .setDataJson(jsonObject.toJSONString());
        Logger.logInfoMessage("send to foundation to create NodeType tx "+ message.toString());
        try {
            RestfulHttpClient.HttpResponse httpResponse = MessageManager.sendMessageToFoundation(message);
            Result responseResult = JSON.parseObject(httpResponse.getContent(), Result.class);
            if (responseResult.getSuccess()) {
                result = true;
                Logger.logInfoMessage("[ OK ] Success to send create poc node type tx message!");
            } else {
                Logger.logWarningMessage("[ WARN ] Failed to send create poc node type tx message! reason: " + responseResult.getMsg());
            }
        } catch (IOException e) {
            Logger.logErrorMessage("[ ERROR ]Failed to send create poc node type tx message!", e);
        }
        return result;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
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
