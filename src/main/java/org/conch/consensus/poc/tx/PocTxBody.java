package org.conch.consensus.poc.tx;

import org.conch.common.ConchException;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.TransactionType;
import org.conch.util.Convert;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public interface PocTxBody  {

     enum WeightTableOptions {
        NODE_TYPE("node"),
        SERVER_OPEN("serverOpen"),
        SS_HOLD("ssHold"),
        HARDWARE_CONFIG("hardwareConfig"),
        NETWORK_CONFIG("networkConfig"),
        TX_PERFORMANCE("txPerformance"),
        BLOCK_MISS("blockMiss"),
        BC_SPEED("bcSpeed"), 
        ONLINE_RATE("onlineRate");
        
        private String value;

        WeightTableOptions(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum DeviceLevels {
        POOR(0), // 极差
        BAD(1), // 可以认为是差、低、丢失量高,等
        MIDDLE(2), // 中
        GOOD(3); // 可以认为是好、高、丢失量低,等
        
        private final int level;

        public int getLevel() {
            return level;
        }

        DeviceLevels(int level) {
            this.level = level;
        }
    }
    
    enum OnlineStatusDef {
        FROM_99_00_TO_99_99(0),
        FROM_97_00_TO_99_00(1),
        FROM_90_00_TO_97_00(2),
        FROM_00_00_TO_97_00(3),
        FROM_00_00_TO_90_00(4),
        FROM_99_00_TO_100(5),
        FROM_97_00_TO_100(6),
        FROM_90_00_TO_100(7);
        
        private final int value;

        public int getValue() {
            return value;
        }

        OnlineStatusDef(int value) {
            this.value = value;
        }
    }


     final class PocNodeType extends Attachment.TxBodyBase {
        private String ip;
        private Peer.Type type;

        public String getIp() {
            return ip;
        }

        public Peer.Type getType() {
            return type;
        }

        public PocNodeType(String ip, Peer.Type type) {
          this.ip = ip;
          this.type = type;
        }


        public PocNodeType(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.type = Peer.Type.getByCode(buffer.getInt());
            try {
                this.ip = Convert.readString(buffer,buffer.getShort(),10);
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        }

        public PocNodeType(JSONObject attachmentData) {
          super(attachmentData);
          this.ip = (String) attachmentData.get("ip");
          this.type =  Peer.Type.getByCode((Integer) attachmentData.get("type"));
        }


        @Override
        public int getMySize() {
            return 4 + 2 + ip.getBytes().length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(type.getCode());

            byte[] ip = Convert.toBytes(this.ip);
            buffer.putShort((short) ip.length);
            buffer.put(ip);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("ip", this.ip);
            attachment.put("type", this.type.getCode());
        }

        @Override
        public TransactionType getTransactionType() {
          return PocTxWrapper.POC_NODE_TYPE;
        }
    }

    final class PocWeightTable extends Attachment.TxBodyBase {

        private Map<String, BigInteger> weightMap;
        private Map<Integer, BigInteger> nodeTypeTemplate;
        private Map<Long, BigInteger> serverOpenTemplate;
        private Map<Integer, BigInteger> hardwareConfigTemplate;
        private Map<Integer, BigInteger> networkConfigTemplate;
        private Map<Integer, BigInteger> txPerformanceTemplate;
        
        private Map<Peer.Type,Map<Integer, BigInteger>> onlineRateTemplate;
        
        private Map<Integer, BigInteger> blockingMissTemplate;
        private Map<Integer, BigInteger> bocSpeedTemplate;
        
        private Long weightTableVersion;

        public Map<String, BigInteger> getWeightMap() {
            return weightMap;
        }

        public Map<Integer, BigInteger> getNodeTypeTemplate() {
            return nodeTypeTemplate;
        }

        public Map<Long, BigInteger> getServerOpenTemplate() {
            return serverOpenTemplate;
        }

        public Map<Integer, BigInteger> getHardwareConfigTemplate() {
            return hardwareConfigTemplate;
        }

        public Map<Integer, BigInteger> getNetworkConfigTemplate() {
            return networkConfigTemplate;
        }

        public Map<Integer, BigInteger> getTxPerformanceTemplate() {
            return txPerformanceTemplate;
        }

        public Map<Peer.Type, Map<Integer, BigInteger>> getOnlineRateTemplate() {
            return onlineRateTemplate;
        }

        public void setOnlineRateTemplate(Map<Peer.Type, Map<Integer, BigInteger>> onlineRateTemplate) {
            this.onlineRateTemplate = onlineRateTemplate;
        }
        
        public Map<Integer, BigInteger> getOnlineRateTemplate(Peer.Type type){
           if(type == null || onlineRateTemplate == null || onlineRateTemplate.size() <= 0) return null;
           return onlineRateTemplate.get(type);
        }

        public Map<Integer, BigInteger> getBlockingMissTemplate() {
            return blockingMissTemplate;
        }

        public Map<Integer, BigInteger> getBocSpeedTemplate() {
            return bocSpeedTemplate;
        }

        public Long getWeightTableVersion() {
            return weightTableVersion;
        }

        public void setWeightTableVersion(Long weightTableVersion) {
            this.weightTableVersion = weightTableVersion;
        }
        

        /**
         * 
         * @return
         */
        public static PocWeightTable defaultPocWeightTable(){

            Map<String, BigInteger> weightMap = new HashMap<>();
            weightMap.put(WeightTableOptions.NODE_TYPE.value, BigInteger.valueOf(25)); // 节点类型占比， 25%，先不算百分比
            weightMap.put(WeightTableOptions.SERVER_OPEN.value, BigInteger.valueOf(20L)); // 开启服务占比，20%， 先不算百分比
            weightMap.put(WeightTableOptions.SS_HOLD.value, BigInteger.valueOf(40L)); // SS持有量占比， 40%，先不算百分比
            weightMap.put(WeightTableOptions.HARDWARE_CONFIG.value, BigInteger.valueOf(5L)); // 硬件配置占比，5%，先不算百分比
            weightMap.put(WeightTableOptions.NETWORK_CONFIG.value, BigInteger.valueOf(5L)); // 网络配置占比， 5%，先不算百分比
            weightMap.put(WeightTableOptions.TX_PERFORMANCE.value, BigInteger.valueOf(5L)); //交易处理性能占比， 5%,先不算百分比
           
            Map<Integer, BigInteger> nodeTypeTP = new HashMap();
            nodeTypeTP.put(Peer.Type.FOUNDATION.getCode(), BigInteger.valueOf(10L)); // 基金会节点
            nodeTypeTP.put(Peer.Type.COMMUNITY.getCode(), BigInteger.valueOf(8L)); // 社区节点 
            nodeTypeTP.put(Peer.Type.HUB.getCode(), BigInteger.valueOf(6L)); // HUB节点 
            nodeTypeTP.put(Peer.Type.BOX.getCode(), BigInteger.valueOf(6L)); // BOX节点 
            nodeTypeTP.put(Peer.Type.NORMAL.getCode(), BigInteger.valueOf(3L)); // 普通节点
            
            Map<Long, BigInteger> serverOpenTP = new HashMap<>();
            serverOpenTP.put(Peer.Service.MINER.getCode(),BigInteger.valueOf(4L)); // 矿工服务开启 
            serverOpenTP.put(Peer.Service.BAPI.getCode(),BigInteger.valueOf(4L)); // 观察者服务开启 
            serverOpenTP.put(Peer.Service.NATER.getCode(),BigInteger.valueOf(4L)); // 穿透者服务开启
            serverOpenTP.put(Peer.Service.STORAGE.getCode(),BigInteger.valueOf(4L)); // 存储者服务开启 
            serverOpenTP.put(Peer.Service.PROVER.getCode(),BigInteger.valueOf(4L)); // 证明者服务开启
            
            Map<Integer, BigInteger> hardwareConfigTP = new HashMap<>();
            hardwareConfigTP.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(3L)); // 硬件配置低 
            hardwareConfigTP.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(6L)); // 硬件配置中 
            hardwareConfigTP.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(10L)); // 硬件配置高 
            
            Map<Integer, BigInteger> networkConfigTP = new HashMap<>();
            networkConfigTP.put(DeviceLevels.POOR.getLevel(), BigInteger.valueOf(0L)); // 网络配置极差 
            networkConfigTP.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(3L)); // 网络配置差 
            networkConfigTP.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(6L)); // 网络配置中 
            networkConfigTP.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(10L)); // 网络配置高 
            
            Map<Integer, BigInteger> txPerformanceTP = new HashMap<>();
            txPerformanceTP.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(3L)); // 交易处理性能低 
            txPerformanceTP.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(6L)); // 交易处理性能中 
            txPerformanceTP.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(10L));  // 交易处理性能高 
            
            Map<Integer, BigInteger> onlineRateFoundationTP = new HashMap<>();
            onlineRateFoundationTP.put(OnlineStatusDef.FROM_99_00_TO_99_99.getValue(), BigInteger.valueOf(-2L)); // 基金会节点在线率1 
            onlineRateFoundationTP.put(OnlineStatusDef.FROM_97_00_TO_99_00.getValue(), BigInteger.valueOf(-5L)); // 基金会节点在线率2 
            onlineRateFoundationTP.put(OnlineStatusDef.FROM_00_00_TO_97_00.getValue(), BigInteger.valueOf(-10L)); // 基金会节点在线率3 
            
            Map<Integer, BigInteger> onlineRateCommunityTP = new HashMap<>();
            onlineRateCommunityTP.put(OnlineStatusDef.FROM_97_00_TO_99_00.getValue(),BigInteger.valueOf(-2L)); // 社区节点在线率1  
            onlineRateCommunityTP.put(OnlineStatusDef.FROM_90_00_TO_97_00.getValue(),BigInteger.valueOf(-5L)); // 社区节点在线率2 
            onlineRateCommunityTP.put(OnlineStatusDef.FROM_00_00_TO_90_00.getValue(),BigInteger.valueOf(-10L)); // 社区节点在线率3  
            
            Map<Integer, BigInteger> onlineRateHubBoxTP = new HashMap<>();
            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_99_00_TO_100.getValue(), BigInteger.valueOf(5L)); // HUB/BOX节点在线率1 
            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_97_00_TO_100.getValue(), BigInteger.valueOf(3L)); // HUB/BOX节点在线率2 
            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_00_00_TO_90_00.getValue(), BigInteger.valueOf(-5L)); // HUB/BOX节点在线率3  
            
            Map<Integer, BigInteger> onlineRateNormalTP = new HashMap<>();
            onlineRateNormalTP.put(OnlineStatusDef.FROM_97_00_TO_100.getValue(), BigInteger.valueOf(5L)); // 普通节点在线率1 
            onlineRateNormalTP.put(OnlineStatusDef.FROM_90_00_TO_100.getValue(), BigInteger.valueOf(3L)); // 普通节点在线率2 
            
            Map<Peer.Type,Map<Integer, BigInteger>> onlineRateMap = new HashMap<>();
            onlineRateMap.put(Peer.Type.FOUNDATION,onlineRateFoundationTP);
            onlineRateMap.put(Peer.Type.COMMUNITY,onlineRateCommunityTP);
            onlineRateMap.put(Peer.Type.HUB,onlineRateHubBoxTP);
            onlineRateMap.put(Peer.Type.BOX,onlineRateHubBoxTP);
            onlineRateMap.put(Peer.Type.NORMAL,onlineRateNormalTP);
            
            Map<Integer, BigInteger> blockingMissTemplate = new HashMap<>();
            blockingMissTemplate.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(-10L)); // 丢失量高
            blockingMissTemplate.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(-6L)); // 丢失量中
            blockingMissTemplate.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(-3L)); // 丢失量低
            
            Map<Integer, BigInteger> bocSpeedTemplate = new HashMap<>();
            bocSpeedTemplate.put(DeviceLevels.POOR.getLevel(), BigInteger.valueOf(-10L)); // 硬分叉 
            bocSpeedTemplate.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(-6L)); // 分叉收敛慢 
            bocSpeedTemplate.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(-3L)); // 分叉收敛中 

            Long version = 20190218L;
                    
            return new PocWeightTable(weightMap,nodeTypeTP,serverOpenTP,hardwareConfigTP,networkConfigTP,txPerformanceTP,onlineRateMap,blockingMissTemplate,bocSpeedTemplate,version);
        }
        
        
        public PocWeightTable(Map<String, BigInteger> weightMap, Map<Integer, BigInteger> nodeTypeTemplate, Map<Long, BigInteger> serverOpenTemplate, Map<Integer, BigInteger> hardwareConfigTemplate, Map<Integer, BigInteger> networkConfigTemplate, Map<Integer, BigInteger> txPerformanceTemplate,Map<Peer.Type, Map<Integer, BigInteger>> onlineRateTemplate, Map<Integer, BigInteger> blockingMissTemplate, Map<Integer, BigInteger> bocSpeedTemplate, Long version) {
            super(0);
            this.weightMap = weightMap;
            this.nodeTypeTemplate = nodeTypeTemplate;
            this.serverOpenTemplate = serverOpenTemplate;
            this.hardwareConfigTemplate = hardwareConfigTemplate;
            this.networkConfigTemplate = networkConfigTemplate;
            this.txPerformanceTemplate = txPerformanceTemplate;
            this.onlineRateTemplate = onlineRateTemplate;
            this.blockingMissTemplate = blockingMissTemplate;
            this.bocSpeedTemplate = bocSpeedTemplate;
            this.weightTableVersion = version;
        }

        private static int MAX_POC_WEIGHT_TABLE_ITEM = 1024;
        public PocWeightTable(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.weightTableVersion = buffer.getLong();
            System.out.println("weightTableVersion=" + weightTableVersion);
            this.weightMap = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.nodeTypeTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.serverOpenTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.hardwareConfigTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.networkConfigTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.txPerformanceTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.onlineRateTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.blockingMissTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
            this.bocSpeedTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_WEIGHT_TABLE_ITEM),Map.class);
        }

        public PocWeightTable(JSONObject attachmentData) {
            super(attachmentData);
            weightTableVersion = (Long) attachmentData.get("templateVersion");
            weightMap = (Map<String, BigInteger>) attachmentData.get("weightMap");
            nodeTypeTemplate = (Map<Integer, BigInteger>) attachmentData.get("nodeTypeTemplate");
            serverOpenTemplate = (Map<Long, BigInteger>) attachmentData.get("serverOpenTemplate");
            hardwareConfigTemplate = (Map<Integer, BigInteger>) attachmentData.get("hardwareConfigTemplate");
            networkConfigTemplate = (Map<Integer, BigInteger>) attachmentData.get("networkConfigTemplate");
            txPerformanceTemplate = (Map<Integer, BigInteger>) attachmentData.get("txPerformanceTemplate");
            onlineRateTemplate = (Map<Peer.Type, Map<Integer, BigInteger>>) attachmentData.get("onlineRateTemplate");
            blockingMissTemplate = (Map<Integer, BigInteger>) attachmentData.get("blockingMissTemplate");
            bocSpeedTemplate = (Map<Integer, BigInteger>) attachmentData.get("bocSpeedTemplate");
             
        }
        

        @Override
        public int getMySize() {
            return 8 + 4 * 9 + Convert.countJsonBytes(weightMap)
                    + Convert.countJsonBytes(nodeTypeTemplate)
                    + Convert.countJsonBytes(serverOpenTemplate)
                    + Convert.countJsonBytes(hardwareConfigTemplate)
                    + Convert.countJsonBytes(networkConfigTemplate)
                    + Convert.countJsonBytes(txPerformanceTemplate)
                    + Convert.countJsonBytes(onlineRateTemplate)
                    + Convert.countJsonBytes(blockingMissTemplate)
                    + Convert.countJsonBytes(bocSpeedTemplate);
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(weightTableVersion);
            Convert.writeMap(buffer,weightMap);
            Convert.writeMap(buffer,nodeTypeTemplate);
            Convert.writeMap(buffer,serverOpenTemplate);
            Convert.writeMap(buffer,hardwareConfigTemplate);
            Convert.writeMap(buffer,networkConfigTemplate);
            Convert.writeMap(buffer,txPerformanceTemplate);
            Convert.writeMap(buffer,onlineRateTemplate);
            Convert.writeMap(buffer,blockingMissTemplate);
            Convert.writeMap(buffer,bocSpeedTemplate);
//            byte[] array = buffer.array();
//            System.out.println( "getMySize method=" + getMySize() + ",size=" + array.length + ",PocWeightTable =>" + Convert.toString(array));
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("templateVersion", weightTableVersion);
            attachment.put("weightMap", weightMap);
            attachment.put("nodeTypeTemplate", nodeTypeTemplate);
            attachment.put("serverOpenTemplate", serverOpenTemplate);
            attachment.put("hardwareConfigTemplate", hardwareConfigTemplate);
            attachment.put("networkConfigTemplate", networkConfigTemplate);
            attachment.put("txPerformanceTemplate", txPerformanceTemplate);
            attachment.put("onlineRateTemplate", onlineRateTemplate);
            attachment.put("blockingMissTemplate", blockingMissTemplate);
            attachment.put("bocSpeedTemplate", bocSpeedTemplate);
        }

        @Override
        public TransactionType getTransactionType() {
          return PocTxWrapper.POC_WEIGHT_TABLE;
        }
    }

    final class PocNodeConf extends Attachment.TxBodyBase {

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
            return PocTxWrapper.POC_NODE_CONF;
        }
    }

    final class PocOnlineRate extends Attachment.TxBodyBase {
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
            return PocTxWrapper.POC_ONLINE_RATE;
        }
    }

    final class PocBlockMiss extends Attachment.TxBodyBase {
        private long missAccountId;
        private int blockMissTimeStamp;

        public PocBlockMiss(long missAccountId) {
            this.missAccountId = missAccountId;
        }

        public long getMissAccountId() {
            return missAccountId;
        }

        public int getBlockMissTimeStamp() {
            return blockMissTimeStamp;
        }

        public PocBlockMiss(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
             this.missAccountId = buffer.getLong();
             this.blockMissTimeStamp = buffer.getInt();
        }

        public PocBlockMiss(JSONObject attachmentData) {
            super(attachmentData);
            this.missAccountId = (long) attachmentData.get("missAccountId");
            this.blockMissTimeStamp = (int) attachmentData.get("blockMissTimeStamp");
        }

        @Override
        public int getMySize() {
            return 8 + 4;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(missAccountId);
            buffer.putInt(blockMissTimeStamp);
        }

        @Override
        public void putMyJSON(JSONObject json) {
              json.put("missAccountId", missAccountId);
              json.put("blockMissTimeStamp", blockMissTimeStamp);
        }

        @Override
        public TransactionType getTransactionType() {
            return PocTxWrapper.POC_BLOCK_MISS;
        }
    }

    /**
     * Bifurcation of convergence for PoC
     */
    final class PocBcSpeed extends Attachment.TxBodyBase {
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

        public PocBcSpeed(String ip, String port, int speed) {
            this.ip = ip;
            this.port = port;
            this.speed = speed;
        }

        public PocBcSpeed(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.ip = buffer.toString();
            this.port = buffer.toString();
            this.speed = buffer.getInt();
        }

        public PocBcSpeed(JSONObject attachmentData) {
            super(attachmentData);
            this.ip = (String) attachmentData.get("ip");
            this.port = (String) attachmentData.get("port");
            this.speed = (int) attachmentData.get("speed");
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
            return PocTxWrapper.POC_BC_SPEED;
        }
    }
}
