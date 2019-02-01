package org.conch.http;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocTemplate;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.TransactionType;
import org.conch.util.Convert;
import org.conch.util.Https;
import org.conch.util.IpUtil;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class PocTxApi {

    public static final class CreateNodeConf extends CreateTransaction {

        static final CreateNodeConf instance = new CreateNodeConf();

        CreateNodeConf() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "nodeconf");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
            String nodeTypeConfigJson = Https.getPostData(request);
            Account account = ParameterParser.getSenderAccount(request);
            String bindRs = request.getParameter("bindRs");
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

            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
            String ip = Convert.emptyToNull(request.getParameter("ip"));
            String port = Convert.emptyToNull(request.getParameter("port"));
            String transactionIdString = Convert.emptyToNull(request.getParameter("transaction"));
            String transactionFullHash = Convert.emptyToNull(request.getParameter("fullHash"));
            boolean includePhasingResult = Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter("includePhasingResult"));
            List<org.json.simple.JSONObject> transactions;
            List<org.json.simple.JSONObject> nodeConfigJson = new ArrayList<>();
            org.json.simple.JSONObject result = new org.json.simple.JSONObject();

            // 先根据ID和哈希查询交易
            boolean searchViaAddress = transactionIdString == null && transactionFullHash == null;
            transactions = GetTransaction.getTransactions(transactionIdString, transactionFullHash, includePhasingResult);
            if (transactions == null) {
                if (searchViaAddress) {
                    // 获得所有的交易，然后匹配地址
                    transactions = GetTransaction.getTransactions(null, includePhasingResult);
                    queryNodeTypeConfigTransactionsViaAddress(nodeConfigJson, ip, port, transactions);
                }
            } else {
                // 根据已查询到的交易，匹配地址
                queryNodeTypeConfigTransactionsViaAddress(nodeConfigJson, ip, port, transactions);
            }
            result.put("data", JSONObject.toJSONString(nodeConfigJson));
            return result;
        }

        private void queryNodeTypeConfigTransactionsViaAddress(List<org.json.simple.JSONObject> nodeConfigJson, String ip, String port, List<org.json.simple.JSONObject> transactions) {
            for (org.json.simple.JSONObject transaction : transactions) {
                byte type = Byte.parseByte(String.valueOf(transaction.get("type")));
                byte subType = Byte.parseByte(String.valueOf(transaction.get("subtype") == null ? 0 : transaction.get("subtype")));
                if (TransactionType.TYPE_POC == type && PocTxWrapper.SUBTYPE_POC_NODE_CONF == subType) {
                    org.json.simple.JSONObject attachment = (org.json.simple.JSONObject) transaction.get("attachment");
                    String attachmentIp = String.valueOf(attachment.get("ip"));
                    String attachmentPort = String.valueOf(attachment.get("port"));
                    boolean getAll = ip == null && port == null;
                    boolean getClearlyAddress = ip != null && port != null && ip.equalsIgnoreCase(attachmentIp) && port.equalsIgnoreCase(attachmentPort);
                    if (getAll || getClearlyAddress) {
                        attachment.put("fullHash", transaction.get("fullHash"));
                        attachment.put("transaction", transaction.get("transaction"));
                        nodeConfigJson.add(attachment);
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

            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }

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

            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
            String templateJson = Https.getPostData(request);
            Account account = ParameterParser.getSenderAccount(request);
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

            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
            String transactionIdString = Convert.emptyToNull(request.getParameter("transaction"));
            String transactionFullHash = Convert.emptyToNull(request.getParameter("fullHash"));
            boolean includePhasingResult = Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter("includePhasingResult"));
            List<org.json.simple.JSONObject> pocWeightTablesJson = new ArrayList<>();
            org.json.simple.JSONObject result = new org.json.simple.JSONObject();
            long version = ParameterParser.getLong(request, "version", Long.MIN_VALUE, Long.MAX_VALUE, false);

            // 先根据交易ID和交易哈希查询
            List<org.json.simple.JSONObject> transactions = GetTransaction.getTransactions(transactionIdString, transactionFullHash, includePhasingResult);
            boolean searchViaVersion = transactionIdString == null && transactionFullHash == null;
            if (transactions == null) {
                if (searchViaVersion) {
                    // 获得所有的交易，按照version查询
                    transactions = GetTransaction.getTransactions(null, includePhasingResult);
                    queryPocTemplateTransactionsViaVersion(pocWeightTablesJson, version, transactions);
                }
            } else {
                // 若ID或哈希都匹配，先查看附件类型是否是PoC，然后匹配version
                queryPocTemplateTransactionsViaVersion(pocWeightTablesJson, version, transactions);
            }
            result.put("data", JSONObject.toJSONString(pocWeightTablesJson));
            return result;
        }

        private void queryPocTemplateTransactionsViaVersion(List<org.json.simple.JSONObject> pocWeightTablesJson, long version, List<org.json.simple.JSONObject> transactions) {
            for(org.json.simple.JSONObject transaction : transactions) {
                byte type = Byte.parseByte(String.valueOf(transaction.get("type")));
                byte subType = Byte.parseByte(String.valueOf(transaction.get("subtype") == null ? 0 : transaction.get("subtype")));
                if (TransactionType.TYPE_POC == type && PocTxWrapper.SUBTYPE_POC_WEIGHT_TABLE == subType) {
                    org.json.simple.JSONObject attachment = (org.json.simple.JSONObject) transaction.get("attachment");
                    Long templateVersion = attachment.get("templateVersion") == null ? 0 : Long.parseLong(String.valueOf(attachment.get("templateVersion")));
                    // 是PoC模板交易但是查询没有指定版本
                    boolean getAllVersion = templateVersion != 0 && version == 0;
                    // 是PoC模板交易，且查询指定了版本，则要根据版本筛选
                    boolean getClearlyVersion = templateVersion != 0 && version != 0 && templateVersion == version;
                    if (getAllVersion || getClearlyVersion) {
                        attachment.put("fullHash", transaction.get("fullHash"));
                        attachment.put("transaction", transaction.get("transaction"));
                        pocWeightTablesJson.add(attachment);
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

            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
            String onlineRateJson = Https.getPostData(request);
            Account account = ParameterParser.getSenderAccount(request);
            Map onlineRateMap = JSONObject.parseObject(onlineRateJson, Map.class);
            String ip = String.valueOf(onlineRateMap.get("ip"));
            String port = String.valueOf(onlineRateMap.get("port"));
            String rate = String.valueOf(onlineRateMap.get("onlineRate"));
            Integer onlineRate = Integer.parseInt(StringUtils.isEmpty(rate)||"null".equalsIgnoreCase(rate) ? "0" : rate);
            Attachment attachment = new PocTxBody.PocOnlineRate(ip, port, onlineRate);

            return createTransaction(request, account, 0, 0, attachment);
        }
    }

    public static final class GetOnlineRate extends APIServlet.APIRequestHandler {

        static final GetOnlineRate instance = new GetOnlineRate();

        GetOnlineRate() {
            super(new APITag[]{APITag.POC}, "onlinerate");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {

            if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
                throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can create this tx");
            }
            String ip = Convert.emptyToNull(request.getParameter("ip"));
            String port = Convert.emptyToNull(request.getParameter("port"));
            String transactionIdString = Convert.emptyToNull(request.getParameter("transaction"));
            String transactionFullHash = Convert.emptyToNull(request.getParameter("fullHash"));
            boolean includePhasingResult = Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter("includePhasingResult"));
            List<org.json.simple.JSONObject> transactions;
            List<org.json.simple.JSONObject> onlineRateJson = new ArrayList<>();
            org.json.simple.JSONObject result = new org.json.simple.JSONObject();

            // 先根据ID和哈希查询交易
            boolean searchViaAddress = transactionIdString == null && transactionFullHash == null;
            transactions = GetTransaction.getTransactions(transactionIdString, transactionFullHash, includePhasingResult);
            if (transactions == null) {
                if (searchViaAddress) {
                    // 获得所有的交易，然后匹配地址
                    transactions = GetTransaction.getTransactions(null, includePhasingResult);
                    queryOnlineRateTransactionsViaAddress(onlineRateJson, ip, port, transactions);
                }
            } else {
                // 根据已查询到的交易，匹配地址
                queryOnlineRateTransactionsViaAddress(onlineRateJson, ip, port, transactions);
            }
            result.put("data", JSONObject.toJSONString(onlineRateJson));
            return result;
        }

        private void queryOnlineRateTransactionsViaAddress(List<org.json.simple.JSONObject> onlineRateJson, String ip, String port, List<org.json.simple.JSONObject> transactions) {
            for(org.json.simple.JSONObject transaction : transactions) {
                byte type = Byte.parseByte(String.valueOf(transaction.get("type")));
                byte subType = Byte.parseByte(String.valueOf(transaction.get("subtype") == null ? 0 : transaction.get("subtype")));
                if (TransactionType.TYPE_POC == type && PocTxWrapper.SUBTYPE_POC_ONLINE_RATE == subType) {
                    org.json.simple.JSONObject attachment = (org.json.simple.JSONObject) transaction.get("attachment");
                    String attachmentIp = String.valueOf(attachment.get("ip"));
                    String attachmentPort = String.valueOf(attachment.get("port"));
                    boolean getAll = ip == null && port == null;
                    boolean getClearlyAddress = ip != null && port != null && ip.equalsIgnoreCase(attachmentIp) && port.equalsIgnoreCase(attachmentPort);
                    if (getAll || getClearlyAddress) {
                        attachment.put("fullHash", transaction.get("fullHash"));
                        attachment.put("transaction", transaction.get("transaction"));
                        onlineRateJson.add(attachment);
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

}
