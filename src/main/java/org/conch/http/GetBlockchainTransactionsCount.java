package org.conch.http;

import org.conch.Conch;
import org.conch.common.ConchException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**********************************************************************************
 * @package org.conch.http
 * @author Marcio Yang
 * @email yx@sharder.org
 * @company Chongqing Morning Whale Technology Co,.LTD
 * @website http://www.ichaoj.com/
 * @creatAt 2019-一月-24 12:33 星期四
 * @tel 17318413650
 * @comment
 **********************************************************************************/
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
            count = Conch.getBlockchain().getTransactionCountByAccount(accountId,type,subtype);
            response.put("count",count);
        }catch(Exception e){
            throw new RuntimeException(e.toString(),e);
        }
        return response;
    }
}
