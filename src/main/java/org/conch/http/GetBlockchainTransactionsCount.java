package org.conch.http;

import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public class GetBlockchainTransactionsCount extends APIServlet.APIRequestHandler {

    static final GetBlockchainTransactionsCount instance = new GetBlockchainTransactionsCount();

    protected GetBlockchainTransactionsCount() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS}, "account","type","subtype");
    }


    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        long accountId = ParameterParser.getAccountId(req,true);

        byte type;
        byte subtype;
        try{
            type = Byte.parseByte(req.getParameter("type"));
        }catch (NumberFormatException e){
            type = -1;
        }
        try{
            subtype = Byte.parseByte(req.getParameter("subtype"));
        }catch (NumberFormatException e){
            subtype = -1;
        }
        int count = 0;
        JSONObject response = new JSONObject();
        try{
            if(type != TransactionType.TYPE_POC) {
                count = Conch.getBlockchain().getTransactionCountByAccount(accountId,type,subtype);
            }

            if(type == -1 || type == TransactionType.TYPE_POC) {
                List<JSONObject> txJsonObjs = GetBlockchainTransactions.checkOrLoadOldPocTxs(accountId, true);
                count += txJsonObjs.size();
            }

            response.put("count",count);
        }catch(Exception e){
            throw new RuntimeException(e.toString(),e);
        }
        return response;
    }
}
