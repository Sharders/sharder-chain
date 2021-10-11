package org.conch.http;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.CheckSumValidator;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.PocTemplate;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.http.handler.QueryTransactionsHandler;
import org.conch.http.handler.impl.QueryTransactionsCondition;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.util.Convert;
import org.conch.util.Https;
import org.conch.util.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * create various of PoC transactions
 *
 * @author CloudSen
 */
@SuppressWarnings("unchecked")
public abstract class PocTxApi {

    private static void iterateTransactions(List<org.json.simple.JSONObject> transactions, List<org.json.simple.JSONObject> dataJsons, QueryTransactionsCondition condition) {
        transactions.forEach(transaction -> {
            byte type = Byte.parseByte(String.valueOf(transaction.get("type")));
            byte subType = Byte.parseByte(String.valueOf(transaction.get("subtype") == null ? 0 : transaction.get("subtype")));
            condition.setType(type).setSubType(subType);
            try {
                QueryTransactionsHandler.Factory.getHandlerByType(condition.getHandleType())
                        .filter(transaction, dataJsons, condition);
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        });
    }

    private static org.json.simple.JSONObject universalGetTransactions(HttpServletRequest request, QueryTransactionsHandler.HandleType handleType) throws ConchException.NotValidException, ParameterException {
        String ip = Convert.emptyToNull(request.getParameter("ip"));
        String port = Convert.emptyToNull(request.getParameter("port"));
        long expectVersion = ParameterParser.getLong(request, "version", Long.MIN_VALUE, Long.MAX_VALUE, false);
        String transactionIdString = Convert.emptyToNull(request.getParameter("transaction"));
        String transactionFullHash = Convert.emptyToNull(request.getParameter("fullHash"));
        boolean includePhasingResult = Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter("includePhasingResult"));
        List<org.json.simple.JSONObject> dataJson = new ArrayList<>();
        org.json.simple.JSONObject result = new org.json.simple.JSONObject();

        // prepare to query
        QueryTransactionsCondition condition = new QueryTransactionsCondition()
                .setIp(ip).setPort(port).setTemplateVersion(expectVersion).setHandleType(handleType);
        // a flag means: instead of querying by transaction id and hash value, just querying by other conditions
        boolean queryByOtherConditions = transactionIdString == null && transactionFullHash == null;
        // Firstly, querying by transaction ID and hash value
        List<org.json.simple.JSONObject> transactions = GetTransaction.getTransactions(transactionIdString, transactionFullHash, includePhasingResult);
        if (transactions == null) {
            if (queryByOtherConditions) {
                /*
                When the transaction's data is not queried by id and hash value,
                all the transactions are queried, and then filtered by other conditions.
                 */
                transactions = GetTransaction.getTransactions(null, includePhasingResult);
                iterateTransactions(transactions, dataJson, condition);
            }
        } else {
            /*
            When the transaction's data is queried by id and hash value,
            then filtered by other conditions.
             */
            iterateTransactions(transactions, dataJson, condition);
        }
        result.put("data", JSONObject.toJSONString(dataJson));
        return result;
    }

    public static final class CreateNodeConf extends CreateTransaction {

        static final CreateNodeConf INSTANCE = new CreateNodeConf();

        CreateNodeConf() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "nodeconf");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) {
            try {
                Preconditions.checkArgument(UrlManager.validFoundationHost(request), "Not valid host! ONLY foundation domain can do this operation!");
                String nodeTypeConfigJson = Https.getPostData(request);
                Account account = Optional.ofNullable(ParameterParser.getSenderAccount(request))
                        .orElseThrow(() -> new ConchException.AccountControlException("account info can not be null!"));
                Account.checkApiAutoTxAccount(Account.rsAccount(account.getId()));
                SystemInfo systemInfo = Optional.ofNullable(JSONObject.parseObject(nodeTypeConfigJson, SystemInfo.class))
                        .orElseThrow(() -> new ConchException.NotValidException("system info can not be null!"));
                Attachment attachment = new PocTxBody.PocNodeConf(systemInfo.getIp(), systemInfo.getPort(), systemInfo);
                Logger.logInfoMessage("creating node config performance tx...");
                Logger.logDebugMessage(Convert.stringTemplate("account id={},address={}; systemInfo:{}", account.getId(), account.getRsAddress(), systemInfo));
                createTransaction(request, account, 0, 0, attachment);
                Logger.logInfoMessage("success to create node config performance tx");
            } catch (Exception e) {
                return ResultUtil.failed(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
            }
            return ResultUtil.ok(Constants.SUCCESS);
        }
    }

    public static final class GetNodeConf extends APIServlet.APIRequestHandler {

        static final GetNodeConf INSTANCE = new GetNodeConf();

        GetNodeConf() {
            super(new APITag[]{APITag.POC}, "nodeconf");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            return universalGetTransactions(request, QueryTransactionsHandler.HandleType.NODE_CONFIG);
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

        static final CreateNodeType INSTANCE = new CreateNodeType();

        CreateNodeType() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "nodetype");
        }

        /**
         * 1. permissionMode：The POC node declare are created by the Foundation node
         * 2. nonPermissionMode：The POC node declare are created by the any node
         */
        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            try {
                Account account = Optional.ofNullable(ParameterParser.getSenderAccount(request))
                        .orElseThrow(() -> new ConchException.AccountControlException("account info can not be null!"));;
                JSONObject nodeTypeJson = new JSONObject();
                if (Conch.isPermissionMode("true".equalsIgnoreCase(request.getParameter("permissionMode")))) {
                    Preconditions.checkArgument(UrlManager.validFoundationHost(request), "Not valid host! ONLY foundation domain can do this operation!");
                    Account.checkApiAutoTxAccount(Account.rsAccount(account.getId()));
                    String nodeTypeJsonStr = Https.getPostData(request);
                    nodeTypeJson = Optional.ofNullable(JSONObject.parseObject(nodeTypeJsonStr))
                            .orElseThrow(() -> new ConchException.NotValidException("node type info can not be null!"));
                } else {
                    Map<String, String[]> nodeTypeJsonMap = request.getParameterMap();
                    for (Map.Entry<String, String[]> entry : nodeTypeJsonMap.entrySet()) {
                        if (entry.getValue() != null) {
                            nodeTypeJson.put(entry.getKey(), entry.getValue()[0]);
                        }
                    }
                }

                if(StringUtils.isEmpty(nodeTypeJson.getString("ip"))
                || StringUtils.isEmpty(nodeTypeJson.getString("type"))) {
                    String errorMsg = "node ip or node type is null" + nodeTypeJson.toString();
                    Logger.logErrorMessage(errorMsg);
                    throw new ConchException.NotValidException(errorMsg);
                }
                
                PocTxBody.PocNodeTypeV2 pocNodeType = null;
                // v2 need account id or account rs
                Long accountId = -1L;
                if(nodeTypeJson.containsKey("bindRs")){
                    accountId = Account.rsAccountToId(nodeTypeJson.getString("bindRs"));
                }else if(nodeTypeJson.containsKey("accountId")){
                    accountId = nodeTypeJson.getLong("accountId");
                }

                if(accountId == -1L) {
                    pocNodeType = CheckSumValidator.isPreAccountsInTestnet(nodeTypeJson.getString("ip"), Conch.getHeight());
                }else{
                    pocNodeType = new PocTxBody.PocNodeTypeV2(
                            nodeTypeJson.getString("ip"),
                            Peer.Type.getBySimpleName(nodeTypeJson.getString("type")),
                            accountId
                    );
                }

                if(pocNodeType == null) {
                    String errorMsg = "can't create node type tx from input data " + nodeTypeJson.toString();
                    Logger.logErrorMessage(errorMsg);
                    throw new ConchException.NotValidException(errorMsg);
                }

                // v3 need disk capacity
                pocNodeType = new PocTxBody.PocNodeTypeV3(pocNodeType, nodeTypeJson.getLong("diskCapacity"));

                Logger.logInfoMessage("creating node type tx %s", pocNodeType.toString());

                long recipientId = (accountId == -1) ? 0 : accountId;

                JSONStreamAware txJson = createTransaction(request, account, recipientId, 0, pocNodeType);
                Logger.logInfoMessage("success to create node type tx " + txJson.toString());
            } catch(ConchException.AccountControlException e){
                Logger.logErrorMessage(e.getMessage());
                return ResultUtil.failed(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
            } catch(ConchException e) {
                Logger.logErrorMessage(ExceptionUtils.getStackTrace(e));
                return ResultUtil.failed(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
            }
            return ResultUtil.ok(Constants.SUCCESS);
        }
    }

    public static final class CreatePocTemplate extends CreateTransaction {

        static final CreatePocTemplate INSTANCE = new CreatePocTemplate();

        CreatePocTemplate() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "weighttable");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Preconditions.checkArgument(UrlManager.validFoundationHost(request), "Not valid host! ONLY foundation domain can do this operation!");
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

        static final GetPocTemplate INSTANCE = new GetPocTemplate();

        GetPocTemplate() {
            super(new APITag[]{APITag.POC}, "weighttable");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            return universalGetTransactions(request, QueryTransactionsHandler.HandleType.POC_TEMPLATE);
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


    public static final class CreateOnlineRate extends CreateTransaction {

        static final CreateOnlineRate INSTANCE = new CreateOnlineRate();

        CreateOnlineRate() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "onlinerate");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Preconditions.checkArgument(UrlManager.validFoundationHost(request), "Not valid host! ONLY foundation domain can do this operation!");
            String onlineRateStr = Https.getPostData(request);
            Account account = ParameterParser.getSenderAccount(request);
            JSONObject onlineRateJson = JSONObject.parseObject(onlineRateStr);
            String ip = String.valueOf(onlineRateJson.get("ip"));
            String port = String.valueOf(onlineRateJson.get("port"));
            String rate = String.valueOf(onlineRateJson.get("onlineRate"));
            Integer netWorkRate = Integer.parseInt(StringUtils.isEmpty(rate) ? "0" : rate);
            Attachment attachment = new PocTxBody.PocOnlineRate(ip, port, netWorkRate);

            return createTransaction(request, account, 0, 0, attachment);
        }
    }

    public static final class GetOnlineRate extends APIServlet.APIRequestHandler {

        static final GetOnlineRate INSTANCE = new GetOnlineRate();

        GetOnlineRate() {
            super(new APITag[]{APITag.POC}, "onlinerate");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            return universalGetTransactions(request, QueryTransactionsHandler.HandleType.ONLINE_RATE);
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
    
    public static final class ReProcessPocTxs extends APIServlet.APIRequestHandler {
        
        static final ReProcessPocTxs INSTANCE = new ReProcessPocTxs();
        
        ReProcessPocTxs() {
            super(new APITag[]{APITag.DEBUG});
        }

        @Override
        @SuppressWarnings("unchecked")
        protected JSONStreamAware processRequest(HttpServletRequest req) {
            org.json.simple.JSONObject response = new org.json.simple.JSONObject();
            try {
                Conch.getPocProcessor().notifySynTxNow();
                response.put("done", true);
            } catch (RuntimeException e) {
                JSONData.putException(response, e);
            }
            return response;
        }

        @Override
        protected final boolean requirePost() {
            return true;
        }

        @Override
        protected boolean requirePassword() {
            return true;
        }

        @Override
        protected boolean allowRequiredBlockParameters() {
            return false;
        }

        @Override
        protected boolean requireBlockchain() {
            return false;
        }

    }
    
    public static final class ResetCertifiedPeers extends APIServlet.APIRequestHandler {
        
        static final ResetCertifiedPeers INSTANCE = new ResetCertifiedPeers();

        ResetCertifiedPeers() {
            super(new APITag[]{APITag.DEBUG});
        }

        @Override
        @SuppressWarnings("unchecked")
        protected JSONStreamAware processRequest(HttpServletRequest req) {
            org.json.simple.JSONObject response = new org.json.simple.JSONObject();
            try {
                Conch.getPocProcessor().resetCertifiedPeers();
                response.put("done", true);
            } catch (RuntimeException e) {
                JSONData.putException(response, e);
            }
            return response;
        }

        @Override
        protected final boolean requirePost() {
            return true;
        }

        @Override
        protected boolean requirePassword() {
            return true;
        }

        @Override
        protected boolean allowRequiredBlockParameters() {
            return false;
        }

        @Override
        protected boolean requireBlockchain() {
            return false;
        }

    }

}
