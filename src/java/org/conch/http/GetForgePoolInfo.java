package org.conch.http;

import org.conch.ConchException;
import org.conch.ForgePool;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetForgePoolInfo extends APIServlet.APIRequestHandler {
    static final GetForgePoolInfo instance = new GetForgePoolInfo();
    private GetForgePoolInfo() {
        super(new APITag[] {APITag.FORGING}, "poolId");
    }
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        long poolId = ParameterParser.getLong(request,"poolId",Long.MIN_VALUE,Long.MAX_VALUE,true);
        ForgePool forgePool = ForgePool.getForgePool(poolId);
        if(forgePool == null){
            JSONObject response  = new JSONObject();
            response.put("errorCode", 1);
            response.put("errorDescription", "forge pool doesn't exists");
            return JSON.prepare(response);
        }else {
            return forgePool.toJSonObject();
        }
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }
}
