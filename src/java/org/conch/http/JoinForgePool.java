package org.conch.http;

import org.conch.Account;
import org.conch.Attachment;
import org.conch.ConchException;
import org.conch.Constants;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class JoinForgePool extends CreateTransaction{
    static final JoinForgePool instance = new JoinForgePool();
    private JoinForgePool() {
        super(new APITag[] {APITag.FORGING, APITag.CREATE_TRANSACTION}, "poolId","period", "amount");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        Account account = ParameterParser.getSenderAccount(request);
        int period = ParameterParser.getInt(request, "period", Constants.FORGE_POOL_DELAY, 65535, true);
        long poolId = ParameterParser.getLong(request,"poolId",Long.MIN_VALUE,Long.MAX_VALUE,true);
        long amount = ParameterParser.getLong(request,"amount",0,Long.MAX_VALUE,true);
        Attachment attachment = new Attachment.ForgePoolJoin(poolId,amount,period);
        return createTransaction(request, account, 0, 0, attachment);
    }
}
