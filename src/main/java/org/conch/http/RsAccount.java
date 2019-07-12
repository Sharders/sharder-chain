package org.conch.http;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class RsAccount extends APIServlet.APIRequestHandler {
    static final RsAccount instance = new RsAccount();
    private  RsAccount(){
        super(new APITag[] {APITag.ACCOUNTS}, "accountId");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ParameterException {
        String rsAccount = ParameterParser.getRsAccount(request);
        JSONObject response = new JSONObject();
        response.put("rsAccount", rsAccount);

        return response;
    }
}
