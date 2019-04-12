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
import org.apache.commons.lang3.StringUtils;
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
    @JSONType(deserializer = PeerTypeEnumDeserializer.class)
    enum Type {
        /**
         *
         */
        BOX(5, "Sharder Box"),
        HUB(4, "Sharder Hub"),
        NORMAL(3, "Normal Node"),
        COMMUNITY(2, "Community Node"),
        FOUNDATION(1, "Foundation Node");
        private final int code;
        private final String name;

        Type(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public static Type getByCode(int code) {
            for (Type _enum : values()) {
                if (_enum.code == code) {
                    return _enum;
                }
            }
            return null;
        }

        public static Type getByCode(String code) {
            if (StringUtils.isEmpty(code)) return null;

            for (Type _enum : values()) {
                if (_enum.code == Integer.valueOf(code).intValue()) {
                    return _enum;
                }
            }

            return null;
        }

        public static Type getTypeByName(String name) {
            return Arrays.stream(values()).filter(type -> type.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        }

        public static Type getTypeBySimpleType(SimpleType simpleType) {
            return Arrays.stream(values()).filter(type -> type.getName().contains(simpleType.getName()))
                    .findFirst().orElse(null);
        }
    }

    enum SimpleType {
        /**
         *
         */
        FOUNDATION(4, "Foundation"),
        COMMUNITY(3, "Community"),
        NORMAL(2, "Normal"),
        BOX(1, "Box"),
        HUB(0, "Hub");

        private final Integer code;
        private final String name;

        SimpleType(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public Integer getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public static SimpleType getSimpleTypeByCode(Integer code) {
            return Arrays.stream(values()).filter(simpleType -> simpleType.getCode().equals(code))
                    .findFirst().orElse(SimpleType.NORMAL);
        }

        public static String getSimpleTypeNameByCode(Integer code) {
            return Arrays.stream(values()).filter(simpleType -> simpleType.getCode().equals(code))
                    .findFirst().map(SimpleType::getName)
                    .orElse(SimpleType.NORMAL.getName());
        }

        public static SimpleType getSimpleTypeByName(String name) {
            return Arrays.stream(values()).filter(simpleType -> simpleType.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
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
        /**
         * 最新的
         */
        UP_TO_DATE,
        /**
         * 下载中
         */
        DOWNLOADING,
        /**
         * 轻客户端
         */
        LIGHT_CLIENT,
        /**
         * 分叉
         */
        FORK
    }

    boolean providesService(Service service);

    boolean providesServices(long services);

    boolean isUseNATService();

    String getHost();

    int getPort();

    String getAnnouncedAddress();

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

    JSONObject send(JSONStreamAware request);

    JSONObject send(JSONStreamAware request, int maxResponseSize);

}
