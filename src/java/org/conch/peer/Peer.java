/*
 * Copyright © 2017 sharder.org.
 * Copyright © 2014-2017 ichaoj.com.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,
 * no part of the COS software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package org.conch.peer;

import org.conch.http.APIEnum;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Set;

public interface Peer extends Comparable<Peer> {

    enum State {
        NON_CONNECTED, CONNECTED, DISCONNECTED
    }

    enum Service {
        HALLMARK(1),                    // Hallmarked node
        PRUNABLE(2),                    // Stores expired prunable messages
        API(4),                         // Provides open API access over http
        API_SSL(8),                     // Provides open API access over https
        CORS(16);                       // API CORS enabled

        private final long code;        // Service code - must be a power of 2

        Service(int code) {
            this.code = code;
        }

        public long getCode() {
            return code;
        }
    }

    enum BlockchainState {
        UP_TO_DATE, //最新的
        DOWNLOADING, //下载中
        LIGHT_CLIENT, //轻客户端
        FORK //分叉
    }

    boolean providesService(Service service);

    boolean providesServices(long services);

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

    boolean isInbound();

    boolean isInboundWebSocket();

    boolean isOutboundWebSocket();

    boolean isOpenAPI();

    boolean isApiConnectable();

    StringBuilder getPeerApiUri();

    String getBlacklistingCause();

    JSONObject send(JSONStreamAware request);

    JSONObject send(JSONStreamAware request, int maxResponseSize);

}
