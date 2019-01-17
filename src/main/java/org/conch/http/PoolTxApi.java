package org.conch.http;

import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.mint.pool.PoolRule;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.tx.Attachment;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
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
            if(!PocProcessorImpl.isHubBind(account.getId()) && !Constants.isDevnet()) {
                String errorDetail = "current account can't create sharder pool, because account[id=" + account.getId() + ",rs=" + account.getRsAddress() + "] is not be bind to hub";
                Logger.logInfoMessage(errorDetail);
                throw new ConchException.NotValidException(errorDetail);
            }
            
            int period = ParameterParser.getInt(req, "period", Constants.SHARDER_POOL_DELAY, 65535, true);
            JSONObject rules = null;
            try {
                String rule = req.getParameter("rule");
                rules = (JSONObject)(new JSONParser().parse(rule));
            } catch (Exception e) {
                Logger.logErrorMessage("cant obtain rule when create forge pool");
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

            if(cid == null){
                return SharderPoolProcessor.getSharderPoolsFromNow();
            }else{
                long creatorId = ParameterParser.getLong(request, "creatorId", Long.MIN_VALUE, Long.MAX_VALUE, true);

                return SharderPoolProcessor.getSharderPoolsFromNowAndDestroy(creatorId);
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

    public static final class GetPoolInfo extends APIServlet.APIRequestHandler {
        static final GetPoolInfo instance = new GetPoolInfo();

        private GetPoolInfo() {
            super(new APITag[]{APITag.FORGING}, "poolId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            long poolId = ParameterParser.getLong(request, "poolId", Long.MIN_VALUE, Long.MAX_VALUE, true);
            SharderPoolProcessor forgePool = SharderPoolProcessor.getSharderPool(poolId);
            if (forgePool == null) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 1);
                response.put("errorDescription", "sharder pool doesn't exists");
                return JSON.prepare(response);
            } else {
                return forgePool.toJSonObject();
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

    public static final class GetPoolRule extends APIServlet.APIRequestHandler {
        static final GetPoolRule instance = new GetPoolRule();

        private GetPoolRule() {
            super(new APITag[]{APITag.FORGING}, "creatorId");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            long creatorId = ParameterParser.getLong(request, "creatorId", Long.MIN_VALUE, Long.MAX_VALUE, true);
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
