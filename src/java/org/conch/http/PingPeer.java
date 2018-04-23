package org.conch.http;

import org.conch.ConchException;
import org.conch.http.APIServlet;
import org.conch.http.APITag;
import org.conch.peer.Peers;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * PingPeer
 *
 * @author wj
 * @date 2018/4/21
 */
public class PingPeer extends APIServlet.APIRequestHandler{
    static final PingPeer instance = new PingPeer();

    private PingPeer() {
        super(new APITag[] {APITag.INFO});
    }


    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        //TODO only sharder agent can request ,add peer type
        JSONObject json = new JSONObject();
        json.put("address",Peers.getLoad().getHost());
        json.put("peerLoad",Peers.getLoad().toJson());
        JSONStreamAware response = JSON.prepare(json);
        return response;
    }
}
