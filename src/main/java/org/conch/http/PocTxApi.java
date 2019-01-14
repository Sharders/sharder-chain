package org.conch.http;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.consensus.poc.PocTemplate;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.util.Convert;
import org.conch.util.Https;
import org.conch.util.IpUtil;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


public abstract class PocTxApi {

    public static final class CreateNodeConf extends CreateTransaction {

        static final CreateNodeConf instance = new CreateNodeConf();

        CreateNodeConf() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "nodeconf");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            String nodeTypeConfigJson = Https.getPostData(request);
            Account account = ParameterParser.getSenderAccount(request);
            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
            SystemInfo systemInfo = JSONObject.parseObject(nodeTypeConfigJson, SystemInfo.class);
            Attachment attachment = new PocTxBody.PocNodeConf(systemInfo.getIp(), systemInfo.getPort(), systemInfo);
            return createTransaction(request, account, 0, 0, attachment);
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
            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
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
            List<org.json.simple.JSONObject> pocWeightTablesJson = new ArrayList<>();
            org.json.simple.JSONObject result = new org.json.simple.JSONObject();

            long version = ParameterParser.getLong(request, "version", Long.MIN_VALUE, Long.MAX_VALUE, false);
            // 先根据交易ID和交易哈希查询
            List<org.json.simple.JSONObject> transactions = GetTransaction.getTransactions(transactionIdString, transactionFullHash, includePhasingResult);
            boolean searchViaVersion = StringUtils.isEmpty(transactionIdString) && StringUtils.isEmpty(transactionFullHash);
            if (transactions == null) {
                if (searchViaVersion) {
                    // 获得所有的交易，按照version查询
                    transactions = GetTransaction.getTransactions(null, includePhasingResult);
                    processPocTransactions(pocWeightTablesJson, version, transactions);
                }
            } else {
                // 若ID或哈希都匹配，先查看附件类型是否是PoC，然后匹配version
                processPocTransactions(pocWeightTablesJson, version, transactions);
            }
            System.out.println(JSONObject.toJSONString(pocWeightTablesJson));
            result.put("data", JSONObject.toJSONString(pocWeightTablesJson));
            return result;
        }

        private void processPocTransactions(List<org.json.simple.JSONObject> pocWeightTablesJson, long version, List<org.json.simple.JSONObject> transactions) {
            org.json.simple.JSONObject pocWeightTableJson;
            PocTxBody.PocWeightTable pocWeightTable;
            for(org.json.simple.JSONObject transaction : transactions) {
                pocWeightTableJson = new org.json.simple.JSONObject();
                org.json.simple.JSONObject attachment = (org.json.simple.JSONObject) transaction.get("attachment");
                Long pocVersion = attachment.get("version.pocWeightTable") == null ? 0:Long.parseLong(attachment.get("version.pocWeightTable").toString());
                // 是POC交易但是查询没有指定版本
                boolean isPocNoVersion = pocVersion != 0 && version == 0;
                // 是POC交易，且查询指定了版本，则要根据版本筛选
                boolean isPocAndVersion = pocVersion != 0 && version != 0 && pocVersion == version;
                if (isPocNoVersion || isPocAndVersion) {
                    pocWeightTable = PocProcessorImpl.instance.getPocWeightTable(pocVersion);
                    if (pocWeightTable != null) {
                        pocWeightTable.putMyJSON(pocWeightTableJson);
                        pocWeightTableJson.put("fullHash", transaction.get("fullHash"));
                        pocWeightTableJson.put("transaction", transaction.get("transaction"));
                        pocWeightTablesJson.add(pocWeightTableJson);
                    }
                }
            }
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
