package org.conch.http;

import org.conch.Account;
import org.conch.Attachment;
import org.conch.ConchException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class Contract extends CreateTransaction {

    static final Contract instance = new Contract();

    private Contract() {
        super(new APITag[]{APITag.CONTRACT, APITag.CREATE_TRANSACTION},
                "isContractCreation", "recipient", "amountNQT", "gasPrice", "gasLimit", "data");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Account account = ParameterParser.getSenderAccount(req);
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        long amountNQT = ParameterParser.getAmountNQT(req);
        Attachment.Contract contract = ParameterParser.getContract(req);
        return createTransaction(req, account, recipient, amountNQT, contract);
    }
}