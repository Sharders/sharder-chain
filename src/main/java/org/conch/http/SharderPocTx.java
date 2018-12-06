package org.conch.http;

import org.conch.account.Account;
import org.conch.common.ConchException;
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

    public static final class NodeConfigurationTx extends CreateTransaction {

        static final NodeConfigurationTx instance = new NodeConfigurationTx();

        NodeConfigurationTx() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "ip", "port", "systemInfo", "deviceInfo");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);

            return null;
        }
    }

    public static final class WeightTx extends CreateTransaction {

        static final WeightTx instance = new WeightTx();

        WeightTx() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "ip", "port", "nodeWeight", "serverWeight", "configWeight", "networkWeight", "tpWeight", "ssHoldWeight", "blockingMissWeight", "bifuractionConvergenceWeight", "onlineRateWeight");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);

            return null;
        }
    }

    public static final class OnlineRateTx extends CreateTransaction{

        static final OnlineRateTx instance = new OnlineRateTx();

        OnlineRateTx() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "ip", "port", "networkRate");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);

            return null;
        }
    }

    public static final class BlockingMissTx extends CreateTransaction{

        static final BlockingMissTx instance = new BlockingMissTx();

        BlockingMissTx() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "ip", "port", "missLevel");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);

            return null;
        }
    }

    public static final class BifuractionConvergenceTx extends CreateTransaction {

        static final BifuractionConvergenceTx instance = new BifuractionConvergenceTx();

        BifuractionConvergenceTx() {
            super(new APITag[]{APITag.POC, APITag.CREATE_TRANSACTION}, "ip", "port", "speed");
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            Account account = ParameterParser.getSenderAccount(request);
            
            return null;
        }
    }
}
