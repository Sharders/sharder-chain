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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.mint.Generator;
import org.conch.peer.Peer;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * @author jiangbubai
 */
public final class GetUserConfig extends APIServlet.APIRequestHandler {

    static final GetUserConfig INSTANCE = new GetUserConfig();
    static final List<String> EXCLUDE_KEYS = Arrays.asList("sharder.adminPassword", "sharder.HubBindPassPhrase");

    private GetUserConfig() {
        super(new APITag[]{APITag.INFO});
    }

    @Override
    @SuppressWarnings("unchecked")
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        Properties prop = new Properties();
        InputStream input = null;
        try {
            String filename = "conf/" + Conch.CONCH_PROPERTIES;
            File file = new File(filename);
            if (!file.exists()) {
                return response;
            }
            input = new FileInputStream(filename);
            prop.load(input);
            Enumeration<?> e = prop.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (EXCLUDE_KEYS.contains(key)) {
                    continue;
                }
                String value = prop.getProperty(key);
                response.put(key, value);
            }
            
            // get node type
            Conch.nodeType = Peer.Type.NORMAL.getSimpleName();
            String getFrom = "default";
            // when os isn't windows and mac, it should be hub/box or server node
            if (!SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_MAC) {
                String filePath = ".hubSetting/.tempCache/.sysCache";
                String userHome = Paths.get(System.getProperty("user.home"), filePath).toString();
                File tempFile = new File(userHome);

                // hub node check if serial number exist
                if (tempFile.exists()) {
                    String num = FileUtils.readFileToString(tempFile, "UTF-8");
                    Conch.nodeType = this.getTypeSimpleName(num);

                    if (!Peer.Type.NORMAL.matchSimpleName(Conch.nodeType)) {
                         Conch.serialNum= num.replaceAll("(\\r\\n|\\n)", "");
                        response.put("sharder.xxx", Conch.serialNum);
                        Logger.logInfoMessage("Hub info => [serialNum: " + Conch.serialNum + " , nodeType: " + Conch.nodeType + "]");
                    }
                    getFrom = "serial number";
                }
            }else {
                Peer.Type type = Conch.getPocProcessor().bindPeerType(Account.rsAccountToId(Generator.getAutoMiningRS()));
                if(type != null) {
                    Conch.nodeType = type.getSimpleName();
                    getFrom = "certified peer map";
                } 
            }
            
            Logger.logDebugMessage("current os is %s and its node type get from %s is %s", SystemUtils.OS_NAME, getFrom, Conch.nodeType);
            response.put("sharder.NodeType", Conch.nodeType);

        } catch (IOException e) {
            response.clear();
            response.put("error", e.getMessage());
            return response;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response;
    }

    /**
     * get simple name according to serial num, default type is node if there is no num exist.
     *
     * @param num serial number
     * @return Peer Type Simple Name
     * @throws IOException
     */
    private String getTypeSimpleName(String num) throws IOException {

        if (StringUtils.isEmpty(num)) {
            return Peer.Type.NORMAL.getSimpleName();
        }

        String url = UrlManager.getFoundationUrl(
                UrlManager.GET_HARDWARE_TYPE_EOLINKER,
                UrlManager.GET_HARDWARE_TYPE_LOCAL,
                UrlManager.GET_HARDWARE_TYPE_PATH
        );

        RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(url)
                .get()
                .addPathParam("serialNum", num.replaceAll("(\\r\\n|\\n)", ""))
                .request();
        com.alibaba.fastjson.JSONObject result = JSON.parseObject(response.getContent());

        Integer nodeTypeCode = Peer.Type.HUB.getSimpleCode();
        if (result.getBoolean(Constants.SUCCESS)) {
            com.alibaba.fastjson.JSONObject data = result.getJSONObject("data");
            if (data == null || data.getInteger("type") == null) {
                return Peer.Type.NORMAL.getSimpleName();
            }

            nodeTypeCode = data.getInteger("type");
        } else {
            Logger.logWarningMessage(String.format("failed to get node type by serial number[%s]!", num));
        }

        return Peer.Type.getSimpleName(nodeTypeCode);
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
