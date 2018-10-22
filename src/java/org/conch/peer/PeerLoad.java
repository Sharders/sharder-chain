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

import org.conch.Conch;
import org.conch.Constants;
import org.conch.http.API;
import org.json.simple.JSONObject;

/**
 * PeerLoad
 *
 * @author xy
 * @date 2018/4/17
 */
public final class PeerLoad {
    private Peer.State state = Peer.State.DISCONNECTED;
    private String host;
    private int port;
    private String uri;
    private int load = -1;
    private long lastUpdate = -1;

    public PeerLoad(String host, int port, int load) {
        this.state = Peer.State.CONNECTED;
        this.host = host;
        this.port = Constants.isTestnet() ? API.TESTNET_API_PORT : Conch.getIntProperty("sharder.apiServerPort");
        this.uri = host == null ? null : "http://" + host + ":" + this.port;
        this.load = load;
        this.lastUpdate = System.currentTimeMillis();
    }

    public long getLastUpdateMin() {
        return lastUpdate == -1 ? -1 : lastUpdate / 1000 / 60;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public int loadUp(int load){
        this.load = this.load == -1 ? load : this.load + load;
        lastUpdate = System.currentTimeMillis();
        return this.load;
    }

    public int loadDown(int load){
        this.load = this.load == -1 ? 0 : this.load - load;
        lastUpdate = System.currentTimeMillis();
        return this.load;
    }

    public Peer.State getState() {
        return state;
    }

    public void setState(Peer.State state) {
        this.state = state;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("host",host);
        json.put("port",port);
        json.put("uri",uri);
        json.put("load",load);
        json.put("lastUpdate",lastUpdate);
        return json;
    }
}
