package org.conch.http;

import org.conch.*;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CreateForgePool extends CreateTransaction {
    static final CreateForgePool instance = new CreateForgePool();

    private CreateForgePool() {
        super(new APITag[] {APITag.FORGING, APITag.CREATE_TRANSACTION}, "period", "rule");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Account account = ParameterParser.getSenderAccount(req);
        int period = ParameterParser.getInt(req, "period", Constants.FORGE_POOL_DELAY, 65535, true);
        JSONObject rules = null;
        try{
            rules = (JSONObject)(new JSONParser().parse(req.getParameter("rule")));
        }catch (Exception e){
            Logger.logErrorMessage("cant obtain rule when create forge pool");
        }
        Map<String,Object> rule = Rule.jsonObjectToMap(rules);
        Attachment attachment = new Attachment.ForgePoolCreate(period,rule);
        return createTransaction(req, account, 0, 0, attachment);
    }
}
