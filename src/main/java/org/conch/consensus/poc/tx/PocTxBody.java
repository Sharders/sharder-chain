package org.conch.consensus.poc.tx;

import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.mint.pool.PoolRule;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.TransactionType;
import org.conch.util.Logger;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public abstract class PocTxBody  {

    public enum WeightTableOptions {
        NODE("node"),
        SERVER_OPEN("serverOpen"),
        SS_HOLD("ssHold"),
        HARDWARE_CONFIG("hardwareConfig"),
        NETWORK_CONFIG("networkConfig"),
        TX_HANDLE_PERFORMANCE("txHandlePerformance"),
        BLOCKING_MISS("blockingMiss"),
        BOC_SPEED("bocSpeed"), // 分叉收敛速度
        ONLINE_RATE("onlineRate")
        ;
        private String optionValue;

        WeightTableOptions(String optionValue) {
            this.optionValue = optionValue;
        }

        public String getOptionValue() {
            return optionValue;
        }
    }

    public enum DeviceLevels {
        POOR(0), // 极差
        BAD(1), // 可以认为是差、低、丢失量高,等
        MIDDLE(2), // 中
        GOOD(3), // 可以认为是好、高、丢失量低,等
        ;
        private Integer level;

        public Integer getLevel() {
            return level;
        }

        DeviceLevels(Integer level) {
            this.level = level;
        }
    }

    private PocTxBody() {}

    //TODO finish the PocNodeType definition
    public final class PocNodeType extends Attachment.TxBodyBase {
        private String ip;
        private Peer.Type type;

        public String getIp() {
            return ip;
        }

        public Peer.Type getType() {
            return type;
        }

        public PocNodeType(
            String ip,
            Peer.Type type) {
          this.ip = ip;
        }


        public PocNodeType(ByteBuffer buffer, byte transactionVersion) {
          super(buffer, transactionVersion);
          this.ip = buffer.toString();
        }

        public PocNodeType(JSONObject attachmentData) {
          super(attachmentData);
          this.ip = (String) attachmentData.get("ip");
        }


        @Override
        protected AbstractAttachment inst(ByteBuffer buffer, byte transactionVersion) {
            return new PocWeightTable(buffer,transactionVersion);
        }

        @Override
        protected AbstractAttachment inst(JSONObject attachmentData) {
            return new PocWeightTable(attachmentData);
        }

        @Override
        protected AbstractAttachment inst(int version) {
            return null;
        }


        @Override
        public int getMySize() {
          return ip.getBytes().length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
          buffer.put(ip.getBytes());
        }

        @Override
        public void putMyJSON(JSONObject attachment) {

        }

        @Override
        public TransactionType getTransactionType() {
          return PocTx.POC_WEIGHT_TABLE;
        }
    }

    public final static class PocWeightTable extends Attachment.TxBodyBase {

        /**
         * eg: scoreMap 结构
         *
         * <p>Map<String, Object> scoreMap = new HashMap<>();
         *
         * <p>Map<Integer, BigInteger> nodeTypeTemplate = new HashMap();
         * nodeTypeTemplate.put(1, new BigInteger.valueOf(10L)); // 基金会节点 -> key对应Peer.Type.OFFICIAL
         * nodeTypeTemplate.put(2, new BigInteger.valueOf(8L)); // 社区节点 -> key对应Peer.Type.COMMUNITY
         * nodeTypeTemplate.put(4, new BigInteger.valueOf(6L)); // HUB节点 -> key对应Peer.Type.HUB
         * nodeTypeTemplate.put(5, new BigInteger.valueOf(6L)); // BOX节点 -> key对应Peer.Type.BOX
         * nodeTypeTemplate.put(3, new BigInteger.valueOf(3L)); // 普通节点 -> key对应Peer.Type.NORMAL
         *
         * <p>scoreMap.put("node", nodeTypeTemplate);
         *
         * <p>Map<Integer, BigInteger> serverOpenTemplate = new HashMap<>();
         * serverOpenTemplate.put(1, new BigInteger.valueOf(4L)); // 矿工服务开启 -> key对应Peer.Type.OFFICIAL
         * serverOpenTemplate.put(2, new BigInteger.valueOf(4L)); // 观察者服务开启 -> key对应Peer.Type.COMMUNITY
         * serverOpenTemplate.put(4, new BigInteger.valueOf(4L)); // 穿透者服务开启 -> key对应Peer.Type.HUB
         * serverOpenTemplate.put(5, new BigInteger.valueOf(4L)); // 存储者服务开启 -> key对应Peer.Type.BOX
         * serverOpenTemplate.put(3, new BigInteger.valueOf(4L)); // 证明者服务开启 -> key对应Peer.Type.NORMAL
         *
         * <p>scoreMap.put("serverOpen", serverOpenTemplate);
         *
         * <p>Map<Integer, BigInteger> hardwareConfigTemplate = new HashMap<>();
         * hardwareConfigTemplate.put(1, new BigInteger.valueOf(3L)); // 硬件配置低 -> key对应DeviceLevels.BAD.getLevel()
         * hardwareConfigTemplate.put(2, new BigInteger.valueOf(6L)); // 硬件配置中 -> key对应DeviceLevels.MIDDLE.getLevel()
         * hardwareConfigTemplate.put(3, new BigInteger.valueOf(10L)); // 硬件配置高 -> key对应DeviceLevels.GOOD.getLevel()
         *
         * <p>scoreMap.put("hardwareConfig", hardwareConfigTemplate);
         *
         * <p>Map<Integer, BigInteger> networkConfigTemplate = new HashMap<>();
         * networkConfigTemplate.put(0, new BigInteger.valueOf(0L)); // 网络配置极差 -> key对应DeviceLevels.POOR.getLevel()
         * networkConfigTemplate.put(1, new BigInteger.valueOf(3L)); // 网络配置差 -> key对应DeviceLevels.BAD.getLevel()
         * networkConfigTemplate.put(2, new BigInteger.valueOf(6L)); // 网络配置中 -> key对应DeviceLevels.MIDDLE.getLevel()
         * networkConfigTemplate.put(3, new BigInteger.valueOf(10L)); // 网络配置高 -> key对应DeviceLevels.GOOD.getLevel()
         *
         * <p>scoreMap.put("networkConfig", networkConfigTemplate);
         *
         * <p>Map<Integer, BigInteger> txHandlePerformanceTemplate = new HashMap<>();
         * txHandlePerformanceTemplate.put(1, new BigInteger.valueOf(3L)); // 交易处理性能低 -> key对应DeviceLevels.BAD.getLevel()
         * txHandlePerformanceTemplate.put(2, new BigInteger.valueOf(6L)); // 交易处理性能中 -> key对应DeviceLevels.MIDDLE.getLevel()
         * txHandlePerformanceTemplate.put(3, new BigInteger.valueOf(10L)); // 交易处理性能高 -> key对应DeviceLevels.GOOD.getLevel()
         *
         * scoreMap.put("txHandlePerformance", txHandlePerformanceTemplate);
         *
         * Map<Integer, BigInteger> onlineRateTemplate = new HashMap<>();
         * txHandlePerformanceTemplate.put(1, new BigInteger.valueOf(-2L)); // 基金会节点在线率1  99%<在线率<99.99%
         * txHandlePerformanceTemplate.put(2, new BigInteger.valueOf(-5L)); // 基金会节点在线率2  97%<在线率<99%
         * txHandlePerformanceTemplate.put(3, new BigInteger.valueOf(-10L)); // 基金会节点在线率3  在线率<97%
         * txHandlePerformanceTemplate.put(4, new BigInteger.valueOf(-2L)); // 社区节点在线率1  97%<在线率<99%
         * txHandlePerformanceTemplate.put(5, new BigInteger.valueOf(-5L)); // 社区节点在线率2  90%<在线率<97%
         * txHandlePerformanceTemplate.put(6, new BigInteger.valueOf(-10L)); // 社区节点在线率3  在线率<90%
         * txHandlePerformanceTemplate.put(7, new BigInteger.valueOf(5L)); // HUB/BOX节点在线率1  99%<在线率
         * txHandlePerformanceTemplate.put(8, new BigInteger.valueOf(3L)); // HUB/BOX节点在线率2  97%<在线率
         * txHandlePerformanceTemplate.put(9, new BigInteger.valueOf(-5L)); // HUB/BOX节点在线率3  在线率<90%
         * txHandlePerformanceTemplate.put(10, new BigInteger.valueOf(5L)); // 普通节点在线率1  97%<在线率
         * txHandlePerformanceTemplate.put(11, new BigInteger.valueOf(3L)); // 普通节点在线率2  90%<在线率
         *
         * scoreMap.put("onlineRate", onlineRateTemplate);
         *
         * Map<Integer, BigInteger> blockingMissTemplate = new HashMap<>();
         * blockingMissTemplate.put(1, new BigInteger.valueOf(-10L)); // 丢失量高 -> key对应DeviceLevels.BAD.getLevel()
         * blockingMissTemplate.put(2, new BigInteger.valueOf(-6L)); // 丢失量中 -> key对应DeviceLevels.MIDDLE.getLevel()
         * blockingMissTemplate.put(3, new BigInteger.valueOf(-3L)); // 丢失量低 -> key对应DeviceLevels.GOOD.getLevel()
         *
         * scoreMap.put("blockingMiss", blockingMissTemplate);
         *
         * Map<Integer, BigInteger> bocSpeedTemplate = new HashMap<>();
         * bocSpeedTemplate.put(0, new BigInteger.valueOf(-10L)); // 硬分叉 -> key对应DeviceLevels.POOR.getLevel()
         * bocSpeedTemplate.put(1, new BigInteger.valueOf(-6L)); // 分叉收敛慢 -> key对应DeviceLevels.BAD.getLevel()
         * bocSpeedTemplate.put(2, new BigInteger.valueOf(-3L)); // 分叉收敛中 -> key对应DeviceLevels.MIDDLE.getLevel()
         *
         * scoreMap.put("bocSpeed", bocSpeedTemplate);
         */
        private Map<String, Object> scoreMap;

        /**
         * eg: weightMap 结构
         * Map<String, BigInteger> weightMap = new HashMap<>();
         *
         * weightMap.put("node", new BigInteger(25L)); // 节点类型占比， 25%，先不算百分比
         * weightMap.put("serverOpen", new BigInteger(20L)); // 开启服务占比，20%， 先不算百分比
         * weightMap.put("ssHold", new BigInteger(40L)); // SS持有量占比， 40%，先不算百分比
         * weightMap.put("hardwareConfig", new BigInteger(5L)); // 硬件配置占比，5%，先不算百分比
         * weightMap.put("networkConfig", new BigInteger(5L)); // 网络配置占比， 5%，先不算百分比
         * weightMap.put("txHandlePerformance", new BigInteger(5L)); //交易处理性能占比， 5%,先不算百分比
         *
         */
        private Map<String, Object> weightMap;
        private Map<Integer, BigInteger> nodeTypeTemplate;
        private Map<Integer, BigInteger> serverOpenTemplate;
        private Map<Integer, BigInteger> hardwareConfigTemplate;
        private Map<Integer, BigInteger> networkConfigTemplate;
        private Map<Integer, BigInteger> txHandlePerformanceTemplate;
        private Map<Integer, BigInteger> onlineRateTemplate;
        private Map<Integer, BigInteger> blockingMissTemplate;
        private Map<Integer, BigInteger> bocSpeedTemplate;

        public Map<String, Object> getScoreMap() {
            return scoreMap;
        }

        public Map<String, Object> getWeightMap() {
            return weightMap;
        }

        public Map<Integer, BigInteger> getNodeTypeTemplate() {
            return nodeTypeTemplate;
        }

        public Map<Integer, BigInteger> getServerOpenTemplate() {
            return serverOpenTemplate;
        }

        public Map<Integer, BigInteger> getHardwareConfigTemplate() {
            return hardwareConfigTemplate;
        }

        public Map<Integer, BigInteger> getNetworkConfigTemplate() {
            return networkConfigTemplate;
        }

        public Map<Integer, BigInteger> getTxHandlePerformanceTemplate() {
            return txHandlePerformanceTemplate;
        }

        public Map<Integer, BigInteger> getOnlineRateTemplate() {
            return onlineRateTemplate;
        }

        public Map<Integer, BigInteger> getBlockingMissTemplate() {
            return blockingMissTemplate;
        }

        public Map<Integer, BigInteger> getBocSpeedTemplate() {
            return bocSpeedTemplate;
        }

        public PocWeightTable(Map<String, Object> scoreMap, Map<String, Object> weightMap) {
            this.scoreMap = scoreMap;
            this.weightMap = weightMap;
            setValues(scoreMap, weightMap);
        }

        public PocWeightTable(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            Map<String,Object> scoreMap = null;
            Map<String,Object> weightMap = null;
            try{
                ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.remaining());
                byteBuffer.put(buffer);

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteBuffer.array()));
                scoreMap = (Map<String,Object>)ois.readObject();
                ois.close();
            }catch (Exception e){
                Logger.logErrorMessage("poc weight create transaction can't load score from byte", e);
            }
            try{
                ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.remaining());
                byteBuffer.put(buffer);

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteBuffer.array()));
                weightMap = (Map<String,Object>)ois.readObject();
                ois.close();
            }catch (Exception e){
                Logger.logErrorMessage("poc weight create transaction can't load weight from byte", e);
            }
            this.scoreMap = scoreMap;
            this.weightMap = weightMap;
            setValues(scoreMap, weightMap);
        }

        public PocWeightTable(JSONObject attachmentData) {
            super(attachmentData);
            this.scoreMap = PoolRule.jsonObjectToMap((JSONObject) attachmentData.get("score"));
            this.weightMap = PoolRule.jsonObjectToMap((JSONObject) attachmentData.get("weight"));
            setValues(scoreMap, weightMap);
        }

        private void setValues (Map<String, Object> scoreMap, Map<String, Object> weightMap) {
            if (weightMap.containsKey(WeightTableOptions.NODE.getOptionValue()) && scoreMap.containsKey(WeightTableOptions.NODE.getOptionValue())) {
                this.nodeTypeTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.NODE.getOptionValue());
            }
            if (weightMap.containsKey(WeightTableOptions.SERVER_OPEN.getOptionValue()) && scoreMap.containsKey(WeightTableOptions.SERVER_OPEN.getOptionValue())) {
                this.serverOpenTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.SERVER_OPEN.getOptionValue());
            }
            if (weightMap.containsKey(WeightTableOptions.HARDWARE_CONFIG.getOptionValue()) && scoreMap.containsKey(WeightTableOptions.HARDWARE_CONFIG.getOptionValue())) {
                this.hardwareConfigTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.HARDWARE_CONFIG.getOptionValue());
            }
            if (weightMap.containsKey(WeightTableOptions.HARDWARE_CONFIG.getOptionValue()) && scoreMap.containsKey(WeightTableOptions.HARDWARE_CONFIG.getOptionValue())) {
                this.networkConfigTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.NETWORK_CONFIG.getOptionValue());
            }
            if (weightMap.containsKey(WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue()) && scoreMap.containsKey(WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue())) {
                this.txHandlePerformanceTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue());
            }
            if (scoreMap.containsKey(WeightTableOptions.ONLINE_RATE.getOptionValue())) {
                this.onlineRateTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.ONLINE_RATE.getOptionValue());
            }
            if (scoreMap.containsKey(WeightTableOptions.BLOCKING_MISS.getOptionValue())) {
                this.blockingMissTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.BLOCKING_MISS.getOptionValue());
            }
            if (scoreMap.containsKey(WeightTableOptions.BOC_SPEED.getOptionValue())) {
                this.bocSpeedTemplate = (Map<Integer, BigInteger>) scoreMap.get(WeightTableOptions.BOC_SPEED.getOptionValue());
            }
        }

        @Override
        protected AbstractAttachment inst(ByteBuffer buffer, byte transactionVersion) {
            return new PocWeightTable(buffer,transactionVersion);
        }

        @Override
        protected AbstractAttachment inst(JSONObject attachmentData) {
            return new PocWeightTable(attachmentData);
        }

        @Override
        protected AbstractAttachment inst(int version) {
            return null;
        }

        @Override
        public int getMySize() {
            return _readByteSize(weightMap) + _readByteSize(scoreMap) + _readByteSize(nodeTypeTemplate) + _readByteSize(serverOpenTemplate) + _readByteSize(hardwareConfigTemplate) + _readByteSize(networkConfigTemplate) + _readByteSize(txHandlePerformanceTemplate) + _readByteSize(onlineRateTemplate) + _readByteSize(blockingMissTemplate) + _readByteSize(bocSpeedTemplate);
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            _putByteSize(buffer, weightMap);
            _putByteSize(buffer, scoreMap);
            _putByteSize(buffer, nodeTypeTemplate);
            _putByteSize(buffer, serverOpenTemplate);
            _putByteSize(buffer, hardwareConfigTemplate);
            _putByteSize(buffer, networkConfigTemplate);
            _putByteSize(buffer, txHandlePerformanceTemplate);
            _putByteSize(buffer, onlineRateTemplate);
            _putByteSize(buffer, blockingMissTemplate);
            _putByteSize(buffer, bocSpeedTemplate);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("weight", weightMap);
            attachment.put("score", scoreMap);
            attachment.put("node", nodeTypeTemplate);
            attachment.put("serverOpen", serverOpenTemplate);
            attachment.put("hardwareConfig", hardwareConfigTemplate);
            attachment.put("networkConfig", networkConfigTemplate);
            attachment.put("txHandlePerformance", txHandlePerformanceTemplate);
            attachment.put("onlineRate", onlineRateTemplate);
            attachment.put("blockingMiss", blockingMissTemplate);
            attachment.put("bocSpeed", bocSpeedTemplate);
        }

        @Override
        public TransactionType getTransactionType() {
          return PocTx.POC_WEIGHT_TABLE;
        }
    }

    public final class PocNodeConf extends Attachment.TxBodyBase {

        private final String ip;
        private final String port;
        private SystemInfo systemInfo;

        public String getIp() {
            return ip;
        }

        public String getPort() {
            return port;
        }

        public SystemInfo getSystemInfo() {
            return systemInfo;
        }

        public PocNodeConf(String ip, String port, SystemInfo systemInfo) {
            this.ip = ip;
            this.port = port;
            this.systemInfo = systemInfo;
        }

        public PocNodeConf(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.ip = buffer.toString();
            this.port = buffer.toString();

            ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
            try {
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object obj = ois.readObject();
                if (obj instanceof SystemInfo) {
                    this.systemInfo = (SystemInfo) obj;
                } else {
                    this.systemInfo = null;
                }
                bais.close();
                ois.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public PocNodeConf(JSONObject attachmentData) {
            super(attachmentData);
            this.ip = (String) attachmentData.get("ip");
            this.port = (String) attachmentData.get("port");
            this.systemInfo = (SystemInfo) attachmentData.get("systemInfo");
        }

        @Override
        protected AbstractAttachment inst(ByteBuffer buffer, byte transactionVersion) {
            return new PocNodeConf(buffer,transactionVersion);
        }

        @Override
        protected AbstractAttachment inst(JSONObject attachmentData) {
            return new PocNodeConf(attachmentData);
        }

        @Override
        protected AbstractAttachment inst(int version) {
            return null;
        }

        @Override
        public int getMySize() {
            return ip.getBytes().length
              + port.getBytes().length
              + _readByteSize(systemInfo);
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.put(ip.getBytes());
            buffer.put(port.getBytes());

            _putByteSize(buffer, systemInfo);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("ip", ip);
            attachment.put("port", port);
            attachment.put("systemInfo", systemInfo);
        }

        @Override
        public TransactionType getTransactionType() {
            return PocTx.POC_NODE_CONFIGURATION;
        }
    }

    public final class PocOnlineRate extends Attachment.TxBodyBase {
        private final String ip;
        private final String port;
        private final int networkRate; // 网络在线率百分比的值乘以 100，用 int 表示, 例 99% = 9900， 99.99% = 9999

        public String getIp() {
            return ip;
        }

        public String getPort() {
            return port;
        }

        public int getNetworkRate() {
            return networkRate;
        }

        public PocOnlineRate(String ip, String port, int networkRate) {
            this.ip = ip;
            this.port = port;
            this.networkRate = networkRate;
        }

        public PocOnlineRate(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.ip = buffer.toString();
            this.port = buffer.toString();
            this.networkRate = buffer.getInt();
        }

        public PocOnlineRate(JSONObject attachmentData) {
            super(attachmentData);
            this.ip = (String) attachmentData.get("ip");
            this.port = (String) attachmentData.get("port");
            this.networkRate = (int) attachmentData.get("networkRate");
        }

        @Override
        protected AbstractAttachment inst(ByteBuffer buffer, byte transactionVersion) {
            return new PocOnlineRate(buffer,transactionVersion);
        }

        @Override
        protected AbstractAttachment inst(JSONObject attachmentData) {
            return new PocOnlineRate(attachmentData);
        }

        @Override
        protected AbstractAttachment inst(int version) {
            return null;
        }

        @Override
        public int getMySize() {
            return 2 + ip.getBytes().length + port.getBytes().length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.put(ip.getBytes());
            buffer.put(port.getBytes());
            buffer.putInt(networkRate);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("ip", ip);
            attachment.put("port", port);
            attachment.put("networkRate", networkRate);
        }

        @Override
        public TransactionType getTransactionType() {
            return PocTx.POC_ONLINE_RATE;
        }
    }

    public final class PocBlockMiss extends Attachment.TxBodyBase {
        private long missAccountId;
//    private long reportAccountId;


        public PocBlockMiss(long missAccountId) {
            this.missAccountId = missAccountId;
        }

        public PocBlockMiss(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
             this.missAccountId = buffer.getLong();
        }

        public PocBlockMiss(JSONObject attachmentData) {
            super(attachmentData);
            this.missAccountId = (long) attachmentData.get("missAccountId");
        }

        @Override
        protected AbstractAttachment inst(ByteBuffer buffer, byte transactionVersion) {
            return new PocBlockMiss(buffer,transactionVersion);
        }

        @Override
        protected AbstractAttachment inst(JSONObject attachmentData) {
            return new PocBlockMiss(attachmentData);
        }

        @Override
        protected AbstractAttachment inst(int version) {
            return null;
        }

        @Override
        public int getMySize() {
            return 2;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
        //      buffer.put(ip.getBytes());
        //      buffer.put(port.getBytes());
        //      buffer.putInt(missLevel);
        }

        @Override
        public void putMyJSON(JSONObject json) {
        //      json.put("ip", ip);
        //      json.put("port", port);
        //      json.put("missCount", missLevel);
        }

        @Override
        public TransactionType getTransactionType() {
            return PocTx.POC_BLOCKING_MISS;
        }
    }

    /**
     * Bifurcation of convergence for PoC
     */
    public final class PocBC extends Attachment.TxBodyBase {
        private final String ip;
        private final String port;
        private final int speed; // 分叉收敛速度 1-硬分叉；2-慢；3-中；4-快

        public String getIp() {
            return ip;
        }

        public String getPort() {
            return port;
        }

        public int getSpeed() {
            return speed;
        }

        public PocBC(String ip, String port, int speed) {
            this.ip = ip;
            this.port = port;
            this.speed = speed;
        }

        public PocBC(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.ip = buffer.toString();
            this.port = buffer.toString();
            this.speed = buffer.getInt();
        }

        public PocBC(JSONObject attachmentData) {
            super(attachmentData);
            this.ip = (String) attachmentData.get("ip");
            this.port = (String) attachmentData.get("port");
            this.speed = (int) attachmentData.get("speed");
        }

        @Override
        protected AbstractAttachment inst(ByteBuffer buffer, byte transactionVersion) {
            return new PocBC(buffer,transactionVersion);
        }

        @Override
        protected AbstractAttachment inst(JSONObject attachmentData) {
            return new PocBC(attachmentData);
        }

        @Override
        protected AbstractAttachment inst(int version) {
            return null;
        }

        @Override
        public int getMySize() {
            return 2 + ip.getBytes().length + port.getBytes().length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.put(ip.getBytes());
            buffer.put(port.getBytes());
            buffer.putInt(speed);
        }

        @Override
        public void putMyJSON(JSONObject json) {
            json.put("ip", ip);
            json.put("port", port);
            json.put("speed", speed);
        }

        @Override
        public TransactionType getTransactionType() {
            return PocTx.POC_BC;
        }
    }
}
