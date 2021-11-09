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

package org.conch.peer;

import com.alibaba.fastjson.annotation.JSONType;
import org.conch.http.APIEnum;
import org.conch.util.PeerTypeEnumDeserializer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Arrays;
import java.util.Set;

/**
 * @author ben-xy
 */
public interface Peer extends Comparable<Peer> {

    /**
     * Type defined and mapping
     * - code and name used by COS
     * - simpleCode and simpleName used by outside system
     */
    @JSONType(deserializer = PeerTypeEnumDeserializer.class)
    enum Type {
        CENTER(5, "Center Node", 1, "Machine"),
        SOUL(4, "Soul Node", 0,"Hub"),
        NORMAL(3, "Normal Node", 2,"Normal"),
        COMMUNITY(2, "Community Node", 3, "Community"),
        FOUNDATION(1, "Community Node",4, "Foundation");
        private final int code;
        private final String name;
        private final int simpleCode;
        private final String simpleName;

        Type(int code, String name, int simpleCode,  String simpleName) {
            this.code = code;
            this.name = name;
            this.simpleCode = simpleCode;
            this.simpleName = simpleName;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public int getSimpleCode() {
            return simpleCode;
        }
        
        public String getSimpleName() {
            return simpleName;
        }

        public boolean matchSimpleName(String simpleName) {
            return this.simpleName.equalsIgnoreCase(simpleName);
        }
        
        public static Type getByCode(int code) {
            for (Type _enum : values()) {
                if (_enum.code == code) {
                    return _enum;
                }
            }
            return null;
        }
        
        public static Type getByName(String name) {
            return Arrays.stream(values()).filter(type -> type.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        }

        public static Type getBySimpleName(String simpleName) {
            return Arrays.stream(values()).filter(type -> type.getSimpleName().equalsIgnoreCase(simpleName))
                    .findFirst().orElse(null);
        }
        
        public static String getSimpleName(int simpleCode) {
            return Arrays.stream(values()).filter(type -> type.getSimpleCode() == (simpleCode))
                    .findFirst().map(Type::getSimpleName)
                    .orElse(Type.NORMAL.getSimpleName());
        }
        
        
    }

    enum State {
        /**
         *
         */
        NON_CONNECTED,
        CONNECTED,
        DISCONNECTED
    }

    enum Service {
        /**
         * Hallmarked node
         */
        HALLMARK(1),
        /**
         * Stores expired prunable messages
         */
        PRUNABLE(2),
        /**
         * Open API access over http
         */
        API(4),
        /**
         *  Open API access over https
         */
        API_SSL(8),
        /**
         * API CORS enabled
         */
        CORS(16),
        /**
         * Business API access over http => watcher role
         */
        BAPI(32),
        /**
         * Off-chain data storage => Storer role
         */
        STORAGE(64),
        /**
         * Proxy mining => Miner role
         */
        MINER(128),
        /**
         * Nat service => Traversal role (TBD)
         */
        NATER(256),
        /**
         * Prove service => Prover role (TBD)
         */
        PROVER(512);
        /**
         * Service code - must be a power of 2
         */
        private final long code;

        Service(int code) {
            this.code = code;
        }

        public long getCode() {
            return code;
        }
    }

    enum BlockchainState {
        //最新的
        UP_TO_DATE,
        //下载中
        DOWNLOADING,
        //轻客户端
        LIGHT_CLIENT,
        //分叉
        FORK,
        //过期
        OBSOLETE,
        //无状态
        NONE,
    }

    enum RunningMode {
        DESKTOP("DESKTOP"),
        COMMAND("COMMAND"),
        LIGHT("LIGHT"),
        OTHERS("OTHERS");
        private final String name;

        RunningMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean matchName(String name) {
            return this.name.equalsIgnoreCase(name);
        }

        public static RunningMode getByCode(int code) {
            return Arrays.stream(values()).filter(mode -> mode.ordinal() == code)
                    .findFirst().orElse(null);
        }

        public static RunningMode getByName(String name) {
            return Arrays.stream(values()).filter(mode -> mode.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        }
    }

    boolean providesService(Service service);

    boolean providesServices(long services);

    String getCosUpdateTime();

    boolean isUseNATService();

    String getHost();

    int getPort();

    String getAnnouncedAddress();

    /**
     * return announced address firstly if announced address exist. 
     * else return host
     * @return
     */
    String getAddress();

    State getState();

    String getVersion();

    String getApplication();

    String getPlatform();

    String getSoftware();

    int getApiPort();

    int getApiSSLPort();

    Set<APIEnum> getDisabledAPIs();

    int getApiServerIdleTimeout();

    BlockchainState getBlockchainState();

    Hallmark getHallmark();

    int getWeight();

    String getBindRsAccount();

    void setBindRsAccount(String bindRsAccount);

    boolean shareAddress();

    boolean isBlacklisted();

    void blacklist(Exception cause);

    void blacklist(String cause);

    void unBlacklist();

    void deactivate();

    void remove();

    long getDownloadedVolume();

    long getUploadedVolume();

    int getLastUpdated();

    int getLastConnectAttempt();

    Type getType();

    void setType(Type type);

    boolean isType(Type type);

    boolean isInbound();

    boolean isInboundWebSocket();

    boolean isOutboundWebSocket();

    boolean isOpenAPI();

    boolean isApiConnectable();

    StringBuilder getPeerApiUri();

    String getBlacklistingCause();

    PeerLoad getPeerLoad();

    JSONObject getBlockSummary();

    JSONObject send(JSONStreamAware request);

    JSONObject send(JSONStreamAware request, int maxResponseSize);

}
