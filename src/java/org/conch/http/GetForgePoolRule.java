package org.conch.http;

import org.conch.ConchException;
import org.conch.Rule;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetForgePoolRule extends APIServlet.APIRequestHandler  {
    static final GetForgePoolRule instance = new GetForgePoolRule();
    private GetForgePoolRule() {
        super(new APITag[] {APITag.FORGING},"creatorId");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        long creatorId = ParameterParser.getLong(request,"creatorId",Long.MIN_VALUE,Long.MAX_VALUE,true);
        return Rule.getTemplate(creatorId);
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
