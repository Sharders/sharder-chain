package org.conch.http;

import org.conch.Account;
import org.conch.Attachment;
import org.conch.ConchException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class QuitForgePool extends CreateTransaction {
    static final QuitForgePool instance = new QuitForgePool();

    private QuitForgePool() {
        super(new APITag[] {APITag.FORGING, APITag.CREATE_TRANSACTION}, "txId", "poolId");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        Account account = ParameterParser.getSenderAccount(request);
        long poolId = ParameterParser.getLong(request,"poolId",Long.MIN_VALUE,Long.MAX_VALUE,true);
        long txId = ParameterParser.getLong(request,"txId",Long.MIN_VALUE,Long.MAX_VALUE,true);
        Attachment attachment = new Attachment.ForgePoolQuit(txId,poolId);
        return createTransaction(request, account, 0, 0, attachment);
    }
}
