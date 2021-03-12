package org.conch.http;

import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.mint.Generator;
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
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/24
 */
public abstract class PoolTxApi {
    
    private static void preCheck() throws ConchException.NotValidException {
        if(!Conch.getBlockchainProcessor().isUpToDate()){
            throw new ConchException.NotValidException("Please wait until the blockchain has finished downloading");
        }
        if(Constants.POOL_OPENING_HEIGHT == -1
        || Conch.getHeight() <= Constants.POOL_OPENING_HEIGHT){
            throw new ConchException.NotValidException("Pool function not opening now, please wait Client to upgrade");
        }
    }
    
    public static final class CreatePoolTx extends CreateTransaction {
        static final CreatePoolTx instance = new CreatePoolTx();

        private CreatePoolTx() {
            super(new APITag[]{APITag.FORGING, APITag.CREATE_TRANSACTION}, "period", "rule");
        }

        private static int calPoolPeriod(HttpServletRequest req,Account account, int[] lifeCycleRule) throws ParameterException {
            if(Constants.isDevnet() && Generator.isBindAddress(account.getRsAddress())) return 100;
            
            return Constants.isDevnet() ? 10 : ParameterParser.getInt(req, "period", lifeCycleRule[0], lifeCycleRule[1], true);
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
            preCheck();
            Account account = ParameterParser.getSenderAccount(req);
            int currentHeight = Conch.getHeight();

            long txId = SharderPoolProcessor.hasProcessingCreateTx(account.getId());
            if(txId != -1){
                throw new ConchException.NotValidException("Account %s has a Create Pool tx[%d] be processing, wait for tx confirmed", account.getRsAddress(), txId);
            }
            SharderPoolProcessor.addProcessingCreateTx(account.getId(), -1);
            
            if (!Conch.getPocProcessor().isCertifiedPeerBind(account.getId(), currentHeight) && !Constants.isDevnet()) {
                String errorDetail = "Can't create a mining pool, because account " + account.getRsAddress() + " is not linked to a certified peer";
                Logger.logInfoMessage(errorDetail);
                throw new ConchException.NotValidException(errorDetail);
            }
            
            String rsAddress = account.getRsAddress();
            if(!Generator.HUB_IS_BIND){
                throw new ConchException.NotValidException("Please finish the hub initialization firstly");
            }
            
            if(!Generator.isBindAddress(account.getRsAddress()) && !Constants.isDevnet()){
                throw new ConchException.NotValidException("Your account " + rsAddress + " isn't this Hub's linked TSS address!");
            }

            if (SharderPoolProcessor.whetherCreatorHasWorkingMinePool(account.getId())) {
                throw new ConchException.NotValidException(rsAddress + " has created a pool already");
            }
            
            if(account.getBalanceNQT() - SharderPoolProcessor.PLEDGE_AMOUNT_NQT - Long.valueOf(req.getParameter("feeNQT")) < 0){
                throw new ConchException.NotValidException("Insufficient account balance");
            }

//            int[] lifeCycleRule = PoolRule.predefinedLifecycle();
//            int period = calPoolPeriod(req, account, lifeCycleRule);
            // never end the pool auto after the Constants.POC_POOL_NEVER_END_HEIGHT
            int period = Integer.MAX_VALUE;
            JSONObject rules = null;
            try {
                String rule = req.getParameter("rule");
                rules = (JSONObject) (new JSONParser().parse(rule));
            } catch (Exception e) {
                Logger.logErrorMessage("Can't obtain rules when create mining pool");
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
            preCheck();
            Account account = ParameterParser.getSenderAccount(request);
            long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);

            SharderPoolProcessor miningPool = SharderPoolProcessor.getPool(poolId);

            for(Consignor consignor : miningPool.getConsignors().values()){
                long joinTxId = 0;
                Account ac = Account.getAccount(consignor.getId());
                List<Consignor.JoinTransaction> lcj= consignor.getTransactions();
                if (lcj.size() <= 0) continue;
                for (int i = 0 ; i < lcj.size(); i++){
                    joinTxId = lcj.get(i).getTransactionId();
                    long txId = SharderPoolProcessor.hasProcessingQuitTx(joinTxId);
                    if(txId != -1){
                        throw new ConchException.NotValidException("Has a QuitPool tx[%d] be processing already, wait for tx confirmed", txId);
                    }
                }

            }

            for(Consignor consignor : miningPool.getConsignors().values()){
                long joinTxId = 0;
                Account ac = Account.getAccount(consignor.getId());
                List<Consignor.JoinTransaction> lcj= consignor.getTransactions();
                if (lcj.size() <= 0) continue;

                for(int i = 0 ; i < lcj.size(); i++ ) {
                    joinTxId = lcj.get(i).getTransactionId();
                    SharderPoolProcessor.addProcessingQuitTx(joinTxId, -1);

                    Attachment attachment = new Attachment.SharderPoolQuit(joinTxId, poolId);

                    createTransaction(request, ac, ac.getId(), 0, attachment);

                }

            }

            long txId = SharderPoolProcessor.hasProcessingDestroyTx(poolId);
            if(txId != -1){
                throw new ConchException.NotValidException("Account %s has a DestroyPool tx[%d] of pool[%d] be processing already, wait for tx confirmed", account.getRsAddress(), txId, poolId);
            }
            SharderPoolProcessor.addProcessingDestroyTx(poolId, -1);
            
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
            preCheck();
            Account account = ParameterParser.getSenderAccount(request);
            long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);
            long joinTxId = ParameterParser.getLong(request, "txId", Long.MIN_VALUE, Long.MAX_VALUE,true);

            long txId = SharderPoolProcessor.hasProcessingQuitTx(joinTxId);
            if(txId != -1){
                throw new ConchException.NotValidException("Has a QuitPool tx[%d] be processing already, wait for tx confirmed", txId);
            }
            SharderPoolProcessor.addProcessingQuitTx(joinTxId, -1);
            
            Attachment attachment = new Attachment.SharderPoolQuit(joinTxId, poolId);
            JSONStreamAware aware = createTransaction(request, account, account.getId(), 0, attachment);
            return aware;
        }
    }

    public static final class JoinPoolTx extends CreateTransaction {
        static final JoinPoolTx instance = new JoinPoolTx();
        
        private JoinPoolTx() {
            super(new APITag[]{APITag.FORGING, APITag.CREATE_TRANSACTION}, "poolId", "period", "amount");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            preCheck();
            JSONStreamAware aware = null;
            try{
                Account account = ParameterParser.getSenderAccount(request);
                
                long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);
                SharderPoolProcessor poolProcessor = SharderPoolProcessor.getPool(poolId);
                
                long[] investmentRule = PoolRule.predefinedInvestment(PoolRule.Role.USER);
                long allowedInvestAmount = investmentRule[1];

                // remain amount of pool
                long[] minerInvestmentRule = PoolRule.predefinedInvestment(PoolRule.Role.MINER);
                long remainAmount = minerInvestmentRule[1] + SharderPoolProcessor.PLEDGE_AMOUNT_NQT 
                        - Constants.ONE_SS - poolProcessor.getPower() - poolProcessor.getJoiningAmount();
                if(remainAmount < allowedInvestAmount) allowedInvestAmount = remainAmount;
                long minInvestAmount = investmentRule[0] > allowedInvestAmount ? allowedInvestAmount : investmentRule[0];
                
                long amount = ParameterParser.getLong(request, "amount", minInvestAmount, allowedInvestAmount, true);

                // account balance check
                if(amount > (account.getBalanceNQT() + account.getUnconfirmedBalanceNQT()) ) {
                    String errorDetail = String.format("Account balance[%d] is smaller than join amount[%d]" 
                            , account.getBalanceNQT()/Constants.ONE_SS, amount/Constants.ONE_SS);
                    Logger.logWarningMessage(errorDetail);
                    throw new ConchException.NotValidException(errorDetail);
                }
                
                // period check
                int period = Integer.MAX_VALUE;
//                int[] lifeCycleRule = PoolRule.predefinedLifecycle();
//                int period = ParameterParser.getInt(request, "period", lifeCycleRule[0], lifeCycleRule[1], true);
                int poolRemainBlocks = poolProcessor.getRemainBlocks();
                if(period > poolRemainBlocks){
                    period = poolRemainBlocks;
                }

                Attachment attachment = new Attachment.SharderPoolJoin(poolId, amount, period);
                aware = createTransaction(request, account, 0, 0, attachment);
                poolProcessor.addJoiningAmount(amount);
            } catch(Exception e){
                Logger.logErrorMessage("JoinPoolTx failed" , e);
                throw e;
            }
            return aware;
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
            SharderPoolProcessor miningPool = SharderPoolProcessor.getPool(poolId);
            if (miningPool == null) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 1);
                response.put("errorDescription", "pool doesn't exists");
                return JSON.prepare(response);
            }
            JSONObject json = miningPool.toJsonObject();
            
            if (StringUtils.isNotEmpty(account)) {
                long accountId = Long.parseUnsignedLong(account);
                Consignor consignor = miningPool.getConsignors().get(accountId);
                long joinAmount = (consignor == null) ? 0 : consignor.getAmount();
           
                if(miningPool.getCreatorId() == accountId){
                    joinAmount += SharderPoolProcessor.PLEDGE_AMOUNT_NQT;
                }

                long rewardAmount = 0;
                try{
                    Map<Long, Long> rewardList = PoolRule.calRewardMapAccordingToRules(miningPool.getCreatorId(), poolId, miningPool.getMintRewards(), miningPool.getConsignorsAmountMap());
                    if(rewardList != null && rewardList.containsKey(accountId)){
                        rewardAmount = rewardList.get(accountId); 
                    }
                }catch(Exception e){
                    Logger.logErrorMessage("can't calculate the investor's mining reward",e);
                }
                
                if(consignor != null) {
                    json.put("consignor", consignor.toJsonObj()); 
                }
                json.put("joinAmount", joinAmount);
                json.put("rewardAmount", rewardAmount);
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
