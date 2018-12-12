package org.conch.http;

import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.tx.Attachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**********************************************************************************
 * @package org.conch.http
 * @author Wolf Tian
 * @email twenbin@sharder.org
 * @company Sharder Foundation
 * @website https://www.sharder.org/
 * @creatAt 2018-Dec-03 15:03 Mon
 * @tel 18716387615
 * @comment
 **********************************************************************************/
public abstract class SharderPocTx {

    public static final class CreateNodeConf extends CreateTransaction {

        static final CreateNodeConf instance = new CreateNodeConf();

        CreateNodeConf() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "ip", "port");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            //TODO 根据传入的参数创建交易
            Account account = ParameterParser.getSenderAccount(request);
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
            Attachment attachment = PocProcessorImpl.getPocConfiguration(ip, port, -1);
            assert attachment != null;
            return createTransaction(request, account, 0, 0, attachment);
        }
    }

    public static final class GetNodeConf extends APIServlet.APIRequestHandler {

        static final GetNodeConf instance = new GetNodeConf();

        GetNodeConf() {
            super(new APITag[]{APITag.POC}, "ip", "port");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            //TODO
            
            return null;
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

    public static final class CreatePocTemplate extends CreateTransaction {

        static final CreatePocTemplate instance = new CreatePocTemplate();

        CreatePocTemplate() {
            //TODO
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "XX", "XX");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            //TODO
            return null;
        }
    }

    public static final class GetPocTemplate extends APIServlet.APIRequestHandler {

        static final GetPocTemplate instance = new GetPocTemplate();

        GetPocTemplate() {
            super(new APITag[]{APITag.POC}, "XX", "XX");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            //TODO

            return null;
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


    public static final class OnlineRate extends CreateTransaction{

        static final OnlineRate instance = new OnlineRate();

        OnlineRate() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "ip", "port");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
            Attachment attachment = PocProcessorImpl.getPocOnlineRate(ip, port, -1);
            assert attachment != null;
            return createTransaction(request, account, 0, 0, attachment);
        }
    }


}
