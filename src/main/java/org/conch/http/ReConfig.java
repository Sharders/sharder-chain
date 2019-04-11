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
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;
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
        boolean needBind = "true".equalsIgnoreCase(req.getParameter("sharder.HubBind"));
        HashMap map = new HashMap(16);
        Enumeration enu = req.getParameterNames();
        long creatorId = Account.rsAccountToId(Conch.getStringProperty("sharder.HubBindAddress"));

        if (SharderPoolProcessor.whetherCreatorHasWorkingMinePool(creatorId)) {
            response.put("reconfiged", false);
            response.put("failedReason", "user has created a working pool, failed to configure settings");
            return response;
        }

        if (!verifyFormData(req, response)) {
            Logger.logErrorMessage("failed to configure settings...");
            return response;
        }



        while(enu.hasMoreElements()) {
            String paraName = (String)enu.nextElement();

            if ("sharder.HubBindPassPhrase".equals(paraName)) {
                String pr = req.getParameter(paraName);
                String ssAddr = Account.rsAccount(pr);
                map.put("sharder.HubBindPassPhrase", pr);
                map.put("sharder.HubBindAddress", ssAddr);
                continue;
            }
            if ("newAdminPassword".equals(paraName)) {
                map.put("sharder.adminPassword", req.getParameter(paraName));
                continue;
            }
            if ("sharder.HubBindPassPhrase".equals(paraName) && needBind && !bindNew && !isInit) {
                map.put("sharder.HubBindPassPhrase", Conch.getStringProperty("sharder.HubBindPassPhrase"));
                continue;
            }
            if (EXCLUDE_PARAMS.contains(paraName)) {
                continue;
            }
            map.put(paraName, req.getParameter(paraName));
        }
        Conch.storePropertiesToFile(map);
        if (restart) {
            new Thread(() -> Conch.restartApplication(null)).start();
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
        if (nodeType.equals(Peer.SimpleType.NORMAL.getName())) {
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
                        .addPostParam("serialNum", Conch.serialNum)
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
                            dbValue = Peer.SimpleType.getSimpleTypeNameByCode(hubSetting.getInteger(pageValue.getKey()));
                        } else {
                            dbValue = hubSetting.getString(pageValue.getKey());
                        }
                        if (!this.doVerify(value, dbValue)) {
                            result = false;
                            response.put("reconfiged", false);
                            response.put("failedReason", "tampered data detected! failed to reconfigure!");
                            break;
                        }
                    }
                } else {
                    result = false;
                    response.put("reconfiged", false);
                    response.put("failedReason", "tampered data detected! failed to reconfigure!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                response.put("reconfiged", false);
                response.put("failedReason", "connection error");
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
