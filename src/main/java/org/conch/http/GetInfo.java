package org.conch.http;

import org.conch.common.ConchException;
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
public class GetInfo extends APIServlet.APIRequestHandler{
    static final GetInfo instance = new GetInfo();

    private GetInfo() {
        super(new APITag[] {APITag.DEBUG});
    }


    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        //TODO only sharder agent can request ,add peer type
        JSONObject json = new JSONObject();
        json.put("address","127.0.0.1");
        json.put("peerLoad",Peers.getMyPeerLoad().toJson());
        json.put("bestPeer",Peers.getBestPeerUri());
        json.put("uri","127.0.0.1:" + API.openAPIPort);
        JSONStreamAware response = JSON.prepare(json);
        return response;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }
}
