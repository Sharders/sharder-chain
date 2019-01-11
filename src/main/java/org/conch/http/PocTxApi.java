package org.conch.http;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.consensus.poc.PocTemplate;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.util.Convert;
import org.conch.util.Https;
import org.conch.util.IpUtil;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public abstract class PocTxApi {

    public static final class CreateNodeConf extends CreateTransaction {

        static final CreateNodeConf instance = new CreateNodeConf();

        CreateNodeConf() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "nodeconf");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            //TODO 根据传入的参数创建交易
            Account account = ParameterParser.getSenderAccount(request);
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
//            Attachment attachment = PocProcessorImpl.getPocConfiguration(ip, port, -1);
//            assert attachment != null;
//            return createTransaction(request, account, 0, 0, attachment);
            return null;
        }
    }

    public static final class GetNodeConf extends APIServlet.APIRequestHandler {

        static final GetNodeConf instance = new GetNodeConf();

        GetNodeConf() {
            super(new APITag[]{APITag.POC}, "nodeconf");
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



    /**
     * Create a node type definition tx
     */
    public static final class CreateNodeType extends CreateTransaction {

        static final CreateNodeType instance = new CreateNodeType();

        CreateNodeType() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "nodetype");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);

            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL()))
            throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");

            String ip = request.getParameter("ip");
            String type = request.getParameter("type");
            Attachment attachment = new PocTxBody.PocNodeType(ip,Peer.Type.getByCode(type));
            return createTransaction(request, account, 0, 0, attachment);
        }
    }


    public static final class CreatePocTemplate extends CreateTransaction {

        static final CreatePocTemplate instance = new CreatePocTemplate();

        CreatePocTemplate() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "weighttable");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            String templateJson = Https.getPostData(request);
            Account account = ParameterParser.getSenderAccount(request);
            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL()))
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            PocTemplate customPocTemp = JSONObject.parseObject(
                    templateJson,
                    PocTemplate.class
                    );
            Attachment attachment = PocTxBody.PocWeightTable.pocWeightTableBuilder(customPocTemp);
            return createTransaction(request, account, attachment);
        }
    }

    public static final class GetPocTemplate extends APIServlet.APIRequestHandler {

        static final GetPocTemplate instance = new GetPocTemplate();

        GetPocTemplate() {
            super(new APITag[]{APITag.POC}, "weighttable");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {

            String transactionIdString = Convert.emptyToNull(request.getParameter("transaction"));
            String transactionFullHash = Convert.emptyToNull(request.getParameter("fullHash"));
            boolean includePhasingResult = "true".equalsIgnoreCase(request.getParameter("includePhasingResult"));
            PocTxBody.PocWeightTable pocWeightTable;
            org.json.simple.JSONObject result = new org.json.simple.JSONObject();


            long version = ParameterParser.getLong(request, "version", Long.MIN_VALUE, Long.MAX_VALUE, false);
            // 先根据交易ID和交易哈希查询
            org.json.simple.JSONObject transation = GetTransaction.getTransaction(transactionIdString, transactionFullHash, includePhasingResult);
            if (transation == null) {
                // 根据version单独获取权重表
                pocWeightTable = PocProcessorImpl.instance.getPocWeightTable(version);
                System.out.println(JSONObject.toJSONString(pocWeightTable));
                result.put("data", JSONObject.toJSONString(pocWeightTable));
                result.put("success", Boolean.TRUE);
            } else {
                // 若ID或哈希都匹配，先查看附件类型是否是PoC
                org.json.simple.JSONObject attachment = (org.json.simple.JSONObject) transation.get("attachment");
                Long pocVersion = attachment.get("version.pocWeightTable") == null ? 0:Long.parseLong(attachment.get("version.pocWeightTable").toString());
                if (pocVersion != 0 && version != 0 && pocVersion == version) {
                    // 类型是PoC，且版本匹配,获取权重表
                    pocWeightTable = PocProcessorImpl.instance.getPocWeightTable(version);
                    result.put("success", Boolean.TRUE);
                    result.put("data", JSONObject.toJSONString(pocWeightTable));
                } else {
                    result.put("success", Boolean.FALSE);
                    result.put("data", "");
                }
            }
            return result;
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


    public static final class CreateOnlineRate extends CreateTransaction{

        static final CreateOnlineRate instance = new CreateOnlineRate();

        CreateOnlineRate() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "onlinerate");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);
            String senderIp = IpUtil.getSenderIp(request);
            String foundationIP = IpUtil.getIp(Conch.getSharderFoundationURL());

            if (foundationIP.equals(senderIp)){
                String[] ips = request.getParameterValues("ips");
                Attachment attachment = new Attachment.SharderOnlineRateCreate(ips);
                return createTransaction(request, account, 0, 0, attachment);
            }
           return null;
        }
    }

    public static final class GetOnlineRate extends APIServlet.APIRequestHandler {

        static final GetOnlineRate instance = new GetOnlineRate();

        GetOnlineRate() {
            super(new APITag[]{APITag.POC}, "onlinerate");
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

}
