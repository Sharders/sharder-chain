package org.conch.http;

import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.NOT_ENABLE_ON_LIGHTCLIENT;

public class OnChain extends CreateTransaction{
    static final OnChain instance = new OnChain();

    private OnChain() {
        super("file", new APITag[] {APITag.DATA_STORAGE, APITag.CREATE_TRANSACTION},
                "name", "description", "type", "channel", "data", "existence_height", "replicated_number");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        if (Constants.isLightClient) {
            throw new ParameterException(NOT_ENABLE_ON_LIGHTCLIENT);
        }
        Account account = ParameterParser.getSenderAccount(req);


        return null;

    }
}
