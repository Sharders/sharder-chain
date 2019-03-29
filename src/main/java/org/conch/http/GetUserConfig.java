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
import org.apache.commons.lang3.SystemUtils;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.peer.Peer;
import org.conch.util.FileUtil;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
            if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
                response.put("sharder.NodeType", Peer.SimpleType.NORMAL.getName());
            } else {
                String filePath = "/root/.hubSetting/.tempCache/.sysCache";
                File tempFile = new File(filePath);
                if (!tempFile.exists()) {
                    response.put("sharder.NodeType", Peer.SimpleType.NORMAL.getName());
                } else {
                    String num = FileUtils.readFileToString(tempFile, "UTF-8");
                    String nodeType = this.getNodeType(num);
                    response.put("sharder.NodeType", nodeType);
                    if (!Peer.SimpleType.NORMAL.getName().equalsIgnoreCase(nodeType)) {
                        response.put("sharder.xxx", num);
                    }
                }
            }
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

    private String getNodeType(String num) throws IOException {
        Integer nodeTypeCode = 0;
        String url = UrlManager.getFoundationUrl(
                UrlManager.GET_HARDWAER_TYPE_EOLINKER,
                UrlManager.GET_HARDWAER_TYPE_LOCAL,
                UrlManager.GET_HARDWAER_TYPE_PATH
        );
        RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(url)
                .get()
                .addPathParam("serialNum", num)
                .request();
        com.alibaba.fastjson.JSONObject result = JSON.parseObject(response.getContent());
        if (result.getBoolean(Constants.SUCCESS)) {
            com.alibaba.fastjson.JSONObject data = result.getJSONObject("data");
            if (data == null) {
                return Peer.SimpleType.NORMAL.getName();
            }
            nodeTypeCode = data.getInteger("type");
            if (nodeTypeCode == null) {
                return Peer.SimpleType.NORMAL.getName();
            }
        }
        Logger.logWarningMessage("Failed to get node type!");
        return Peer.SimpleType.getSimpleTypeNameByCode(nodeTypeCode);
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
