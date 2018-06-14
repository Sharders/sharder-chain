package org.conch.http;

import org.conch.Account;
import org.conch.Attachment;
import org.conch.ConchException;
import org.conch.Constants;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class CreateForgePool extends CreateTransaction {
    static final CreateForgePool instance = new CreateForgePool();

    private CreateForgePool() {
        super(new APITag[] {APITag.FORGING, APITag.CREATE_TRANSACTION}, "period", "rule");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Account account = ParameterParser.getSenderAccount(req);
        //TODO validate parameter
        int period = ParameterParser.getInt(req, "period", Constants.FORGE_POOL_DELAY, 65535, true);

        Attachment attachment = new Attachment.ForgePoolCreate(period);
        return createTransaction(req, account, 0, 0, attachment);
    }
}
