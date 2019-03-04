package org.conch.http;

import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.mint.pool.Consignor;
import org.conch.mint.pool.PoolRule;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.tx.Attachment;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/24
 */
public abstract class PoolTxApi {

    public static final class CreatePoolTx extends CreateTransaction {
        static final CreatePoolTx instance = new CreatePoolTx();

        private CreatePoolTx() {
            super(new APITag[]{APITag.FORGING, APITag.CREATE_TRANSACTION}, "period", "rule");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
            Account account = ParameterParser.getSenderAccount(req);
            if (!PocProcessorImpl.isCertifiedPeerBind(account.getId()) && !Constants.isDevnet()) {
                String errorDetail = "current account can't create mint pool, because account[id=" + account.getId() + ",rs=" + account.getRsAddress() + "] is not be bind to certified peer";
                Logger.logInfoMessage(errorDetail);
                throw new ConchException.NotValidException(errorDetail);
            }
            if(account.getBalanceNQT() - SharderPoolProcessor.PLEDGE_AMOUNT - Long.valueOf(req.getParameter("feeNQT")) < 0){
                throw new ConchException.NotValidException("Insufficient account balance");
            }
            int period = Constants.isDevnet() ? 5 : ParameterParser.getInt(req, "period", Constants.SHARDER_POOL_DELAY, 65535, true);
            JSONObject rules = null;
            try {
                String rule = req.getParameter("rule");
                rules = (JSONObject) (new JSONParser().parse(rule));
            } catch (Exception e) {
                Logger.logErrorMessage("cant obtain rule when create mint pool");
            }
            Map<String, Object> rule = PoolRule.jsonObjectToMap(rules);
            Attachment attachment = new Attachment.SharderPoolCreate(period, rule);
            return createTransaction(req, account, 0, 0, attachment);
        }
    }

    public static final class DestroyPoolTx extends CreateTransaction {
        static final DestroyPoolTx instance = new DestroyPoolTx();

        private DestroyPoolTx() {
            super(new APITag[]{APITag.FORGING, APITag.CREATE_TRANSACTION}, "poolId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);
            long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);
            Attachment attachment = new Attachment.SharderPoolDestroy(poolId);
            return createTransaction(request, account, 0, 0, attachment);
        }
    }


    public static final class QuitPoolTx extends CreateTransaction {
        static final QuitPoolTx instance = new QuitPoolTx();

        private QuitPoolTx() {
            super(new APITag[]{APITag.FORGING, APITag.CREATE_TRANSACTION}, "txId", "poolId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);
            long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);
            long txId = ParameterParser.getLong(request, "txId", Long.MIN_VALUE, Long.MAX_VALUE, true);
            Attachment attachment = new Attachment.SharderPoolQuit(txId, poolId);
            return createTransaction(request, account, 0, 0, attachment);
        }
    }

    public static final class JoinPoolTx extends CreateTransaction {
        static final JoinPoolTx instance = new JoinPoolTx();

        private JoinPoolTx() {
            super(new APITag[]{APITag.FORGING, APITag.CREATE_TRANSACTION}, "poolId", "period", "amount");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);
            int period = ParameterParser.getInt(request, "period", Constants.SHARDER_POOL_DELAY, 65535, true);
            long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);
            long amount = ParameterParser.getLong(request, "amount", 0, Long.MAX_VALUE, true);
            Attachment attachment = new Attachment.SharderPoolJoin(poolId, amount, period);
            return createTransaction(request, account, 0, 0, attachment);
        }
    }

    public static final class GetPools extends APIServlet.APIRequestHandler {
        static final GetPools instance = new GetPools();

        private GetPools() {
            super(new APITag[]{APITag.FORGING}, "creatorId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {

            String cid = Convert.emptyToNull(request.getParameter("creatorId"));

            if (cid == null) {
                return sortPools(request, SharderPoolProcessor.getPoolsFromNow());
            } else {
                long creatorId = ParameterParser.getLong(request, "creatorId", Long.MIN_VALUE, Long.MAX_VALUE, true);

                return SharderPoolProcessor.getPoolsFromNowAndDestroy(creatorId);
            }

        }

        /**
         * 排序矿池 列表
         *
         * @param request
         * @param pools
         * @return
         */
        private JSONObject sortPools(HttpServletRequest request, JSONObject pools) {
            String sort = request.getParameter("sort");
            if (sort == null || "default".equals(sort)) {
                return pools;
            }
            JSONArray jsonArray = (JSONArray) (pools.get("pools"));
            Comparator<JSONObject> comparator = null;
            if ("capacity".equals(sort)) {
                comparator = new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        Long a = getLevel(o1).getJSONObject("consignor").getJSONObject("amount").getLong("max");
                        Long b = getLevel(o2).getJSONObject("consignor").getJSONObject("amount").getLong("max");
                        if (a > b) {
                            return -1;
                        } else if (a < b) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                };
            }
            if ("distribution".equals(sort)) {
                comparator = new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        Double a = getLevel(o1).getJSONObject("forgepool").getJSONObject("reward").getDouble("max");
                        Double b = getLevel(o2).getJSONObject("forgepool").getJSONObject("reward").getDouble("max");
                        if (a > b) {
                            return -1;
                        } else if (a < b) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                };
            }
            if ("time".equals(sort)) {
                comparator = new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        Long a = Long.valueOf(o1.get("endBlockNo").toString());
                        Long b = Long.valueOf(o2.get("endBlockNo").toString());
                        if (a > b) {
                            return -1;
                        } else if (a < b) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                };
            }
            jsonArray.sort(comparator);
            return pools;
        }

        /**
         * 获得rule 对象的 level1 或 level0
         *
         * @param rule
         * @return
         */
        private com.alibaba.fastjson.JSONObject getLevel(JSONObject rule) {
            com.alibaba.fastjson.JSONObject o = (com.alibaba.fastjson.JSONObject)com.alibaba.fastjson.JSONObject.toJSON(rule.get("rule"));
            return o.getJSONObject("level1") != null ? o.getJSONObject("level1") : o.getJSONObject("level0");
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

    public static final class GetPoolInfo extends APIServlet.APIRequestHandler {
        static final GetPoolInfo instance = new GetPoolInfo();

        private GetPoolInfo() {
            super(new APITag[]{APITag.FORGING}, "poolId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);
            String account = request.getParameter("account");
            SharderPoolProcessor forgePool = SharderPoolProcessor.getPool(poolId);
            if (forgePool == null) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 1);
                response.put("errorDescription", "sharder pool doesn't exists");
                return JSON.prepare(response);
            }
            JSONObject json = forgePool.toJsonObject();
            if (account != null) {
                Consignor consignor = forgePool.getConsignors().get(Long.parseUnsignedLong(account));
                json.put("joinAmount", consignor == null ? 0 : consignor.getAmount());
            }
            return json;
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

    public static final class GetPoolRule extends APIServlet.APIRequestHandler {
        static final GetPoolRule instance = new GetPoolRule();

        private GetPoolRule() {
            super(new APITag[]{APITag.FORGING}, "creatorId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            long creatorId = Long.parseUnsignedLong(request.getParameter("creatorId"));
            if (!PocProcessorImpl.isCertifiedPeerBind(creatorId) && !Constants.isDevnet()) {
                String errorDetail = "The account is not bound to an authentication peer";
                Logger.logInfoMessage(errorDetail);
                throw new ConchException.NotValidException(errorDetail);
            }
            return PoolRule.getTemplate(creatorId);
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
