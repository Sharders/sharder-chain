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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.conch.Conch;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.util.Logger;
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
 * @date 2019-04-19 updated by Ben
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
            String nodeType = Conch.getNodeType();
            String serialNum = Conch.getSerialNum();

            Logger.logDebugMessage("current os is %s, node type is %s, serial is %s", SystemUtils.OS_NAME, nodeType, (StringUtils.isEmpty(serialNum) ? "null" : serialNum));
            response.put("sharder.NodeType", nodeType);
            response.put("sharder.phase", Conch.STAGE);
            response.put("sharder.xxx", serialNum);

            if(Conch.systemInfo == null) GetNodeHardware.readSystemInfo();
            response.put("sharder.diskCapacity", Conch.systemInfo.getHardDiskSize());
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
    

    
    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }
}
