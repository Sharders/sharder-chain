package org.conch.http;

import com.alibaba.fastjson.JSONObject;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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
//            Attachment attachment = PocProcessorImpl.getPocConfiguration(ip, port, -1);
//            assert attachment != null;
//            return createTransaction(request, account, 0, 0, attachment);
            return null;
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
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "score", "weight");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);
            Map<String, Object> scoreMap = null;
            Map<String, BigInteger> weightMap = null;
            try {
                scoreMap = (Map<String, Object>) JSONObject.parse(request.getParameter("score"));
                weightMap = (Map<String, BigInteger>) JSONObject.parse(request.getParameter("weight"));
            } catch (Exception e) {
                Logger.logErrorMessage("cant obtain score when create score template");
            }
            if (scoreMap != null && weightMap != null) {
                Map<Integer, BigInteger> nodeTypeTemplate = new HashMap<>();
                Map<Integer, BigInteger> serverOpenTemplate = new HashMap<>();
                Map<Integer, BigInteger> hardwareConfigTemplate = new HashMap<>();
                Map<Integer, BigInteger> networkConfigTemplate = new HashMap<>();
                Map<Integer, BigInteger> txHandlePerformanceTemplate = new HashMap<>();
                Map<Integer, Object> onlineRateTemplate;
                Map<Integer, BigInteger> onlineRateOfficialTemplate = new HashMap<>();
                Map<Integer, BigInteger> onlineRateCommunityTemplate = new HashMap<>();
                Map<Integer, BigInteger> onlineRateHubBoxTemplate = new HashMap<>();
                Map<Integer, BigInteger> onlineRateNormalTemplate = new HashMap<>();
                Map<Integer, BigInteger> blockingMissTemplate = new HashMap<>();
                Map<Integer, BigInteger> bocSpeedTemplate = new HashMap<>();
                if (weightMap.containsKey(PocTxBody.WeightTableOptions.NODE.getOptionValue()) && scoreMap.containsKey(PocTxBody.WeightTableOptions.NODE.getOptionValue())) {
                    nodeTypeTemplate = (Map<Integer, BigInteger>) scoreMap.get(PocTxBody.WeightTableOptions.NODE.getOptionValue());
                }
                if (weightMap.containsKey(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue()) && scoreMap.containsKey(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue())) {
                    serverOpenTemplate = (Map<Integer, BigInteger>) scoreMap.get(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue());
                }
                if (weightMap.containsKey(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue()) && scoreMap.containsKey(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue())) {
                    hardwareConfigTemplate = (Map<Integer, BigInteger>) scoreMap.get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue());
                }
                if (weightMap.containsKey(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue()) && scoreMap.containsKey(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue())) {
                    networkConfigTemplate = (Map<Integer, BigInteger>) scoreMap.get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getOptionValue());
                }
                if (weightMap.containsKey(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue()) && scoreMap.containsKey(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue())) {
                    txHandlePerformanceTemplate = (Map<Integer, BigInteger>) scoreMap.get(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue());
                }
                if (scoreMap.containsKey(PocTxBody.WeightTableOptions.ONLINE_RATE.getOptionValue())) {
                    onlineRateTemplate = (Map<Integer, Object>) scoreMap.get(PocTxBody.WeightTableOptions.ONLINE_RATE.getOptionValue());
                    onlineRateOfficialTemplate = (Map<Integer, BigInteger>) onlineRateTemplate.get(Peer.Type.OFFICIAL.getCode());
                    onlineRateCommunityTemplate = (Map<Integer, BigInteger>) onlineRateTemplate.get(Peer.Type.COMMUNITY.getCode());
                    onlineRateHubBoxTemplate = (Map<Integer, BigInteger>) onlineRateTemplate.get(Integer.valueOf(Peer.Type.HUB.getCode() + "" + Peer.Type.BOX.getCode()));
                    onlineRateNormalTemplate = (Map<Integer, BigInteger>) onlineRateTemplate.get(Peer.Type.NORMAL.getCode());
                }
                if (scoreMap.containsKey(PocTxBody.WeightTableOptions.BLOCK_MISS.getOptionValue())) {
                    blockingMissTemplate = (Map<Integer, BigInteger>) scoreMap.get(PocTxBody.WeightTableOptions.BLOCK_MISS.getOptionValue());
                }
                if (scoreMap.containsKey(PocTxBody.WeightTableOptions.BOC_SPEED.getOptionValue())) {
                    bocSpeedTemplate = (Map<Integer, BigInteger>) scoreMap.get(PocTxBody.WeightTableOptions.BOC_SPEED.getOptionValue());
                }
                Attachment attachment = new PocTxBody.PocWeightTable(weightMap, nodeTypeTemplate, serverOpenTemplate, hardwareConfigTemplate, networkConfigTemplate, txHandlePerformanceTemplate, onlineRateOfficialTemplate, onlineRateCommunityTemplate, onlineRateHubBoxTemplate, onlineRateNormalTemplate, blockingMissTemplate, bocSpeedTemplate);
                return createTransaction(request, account, 0, 0, attachment);
            }
            return null;
        }
    }

    public static final class GetPocTemplate extends APIServlet.APIRequestHandler {

        static final GetPocTemplate instance = new GetPocTemplate();

        GetPocTemplate() {
            super(new APITag[]{APITag.POC}, "templateId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            long templateId = ParameterParser.getLong(request, "templateId", Long.MIN_VALUE, Long.MAX_VALUE, true);
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
           return null;
        }
    }


}
