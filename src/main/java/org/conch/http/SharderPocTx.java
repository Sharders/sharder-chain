package org.conch.http;

import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.hardware.DeviceInfo;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.tx.Attachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

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
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
            SystemInfo systemInfo = ParameterParser.getSystemInfo(request);
            DeviceInfo deviceInfo = ParameterParser.getDeviceInfo(request);
            Attachment attachment = new Attachment.PocNodeConfiguration(ip, port, systemInfo, deviceInfo);
            return createTransaction(request, account, 0, 0, attachment);
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
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
            BigInteger nodeWeight = BigInteger.valueOf(ParameterParser.getLong(request, "nodeWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            BigInteger serverWeight = BigInteger.valueOf(ParameterParser.getLong(request, "serverWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            BigInteger configWeight = BigInteger.valueOf(ParameterParser.getLong(request, "configWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            BigInteger tpWeight = BigInteger.valueOf(ParameterParser.getLong(request, "tpWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            BigInteger ssHoldWeight = BigInteger.valueOf(ParameterParser.getLong(request, "ssHoldWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            BigInteger blockingMissWeight = BigInteger.valueOf(ParameterParser.getLong(request, "blockingMissWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            BigInteger bifuractionConvergenceWeight = BigInteger.valueOf(ParameterParser.getLong(request, "bifuractionConvergenceWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            BigInteger onlineRateWeight = BigInteger.valueOf(ParameterParser.getLong(request, "onlineRateWeight", Long.MIN_VALUE, Long.MAX_VALUE, true));
            Attachment attachment = new Attachment.PocWeight(ip, port, nodeWeight, serverWeight, configWeight, nodeWeight, tpWeight, ssHoldWeight, blockingMissWeight, bifuractionConvergenceWeight, onlineRateWeight);
            return createTransaction(request, account, 0, 0, attachment);
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
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
            int networkRate = ParameterParser.getInt(request, "networkRate", 0, Integer.MAX_VALUE, true);
            Attachment attachment = new Attachment.PocOnlineRate(ip, port, networkRate);
            return createTransaction(request, account, 0, 0, attachment);
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
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
            int missLevel = ParameterParser.getInt(request, "missLevel", 0, Integer.MAX_VALUE, true);
            Attachment attachment = new Attachment.PocBlockingMiss(ip, port, missLevel);
            return createTransaction(request, account, 0, 0, attachment);
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
            String ip = request.getParameter("ip");
            String port = request.getParameter("port");
            int speed = ParameterParser.getInt(request, "speed", 0, Integer.MAX_VALUE, true);
            Attachment attachment = new Attachment.PocBifuractionOfConvergence(ip, port, speed);
            return createTransaction(request, account, 0, 0, attachment);
        }
    }
}
