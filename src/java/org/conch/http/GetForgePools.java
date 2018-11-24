package org.conch.http;

import org.conch.ConchException;
import org.conch.pool.SharderPoolProcessor;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetForgePools extends APIServlet.APIRequestHandler {
    static final GetForgePools instance = new GetForgePools();
    private GetForgePools() {
        super(new APITag[] {APITag.FORGING}, "creatorId");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        long creatorId = ParameterParser.getLong(request,"creatorId",Long.MIN_VALUE,Long.MAX_VALUE,true);
        return SharderPoolProcessor.getSharderPoolsFromNowAndDestroy(creatorId);
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
