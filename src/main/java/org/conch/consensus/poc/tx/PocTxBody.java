package org.conch.consensus.poc.tx;

import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocTemplate;
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
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public interface PocTxBody  {
     int MAX_POC_ITEM_BYTEBUFFER = 10240;
     
     enum WeightTableOptions {
        NODE_TYPE("node"),
        SERVER_OPEN("serverOpen"),
        SS_HOLD("ssHold"),
        HARDWARE_CONFIG("hardwareConfig"),
        NETWORK_CONFIG("networkConfig"),
        TX_PERFORMANCE("txPerformance"),
        GENERATION_MISSING("generationMissing"),
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
        // 极差
        POOR(0),
        // 可以认为是差、低、丢失量高,等
        BAD(1),
        // 中
        MIDDLE(2),
        // 可以认为是好、高、丢失量低,等
        GOOD(3);
        
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
        
        private Map<Integer, BigInteger> generationMissingTemplate;
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
           if(type == null || onlineRateTemplate == null || onlineRateTemplate.size() <= 0) {
               return null;
           }
           return onlineRateTemplate.get(type);
        }

        public Map<Integer, BigInteger> getGenerationMissingTemplate() {
            return generationMissingTemplate;
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
         * 根据权重表模板生成权重表
         * @author : yyunsen
         * @date : 2019/1/8 20:41
         * @param pocTemplate : 默认模板或自定义模板
         * @return : org.conch.consensus.poc.tx.PocTxBody.PocWeightTable 权重表
         */
        public static PocWeightTable pocWeightTableBuilder(PocTemplate pocTemplate) {
            Map<String, BigInteger> weightMap = new HashMap<>();
            weightMap.put(WeightTableOptions.NODE_TYPE.value, BigInteger.valueOf(pocTemplate.getNodeTypeWeight()));
            weightMap.put(WeightTableOptions.SERVER_OPEN.value, BigInteger.valueOf(pocTemplate.getServerOpenWeight()));
            weightMap.put(WeightTableOptions.SS_HOLD.value, BigInteger.valueOf(pocTemplate.getSsHoldWeight()));
            weightMap.put(WeightTableOptions.HARDWARE_CONFIG.value, BigInteger.valueOf(pocTemplate.getHardwareConfWeight()));
            weightMap.put(WeightTableOptions.NETWORK_CONFIG.value, BigInteger.valueOf(pocTemplate.getNetWorkConfWeight()));
            weightMap.put(WeightTableOptions.TX_PERFORMANCE.value, BigInteger.valueOf(pocTemplate.getTxHandlePerformanceWeight())); 

            Map<Integer, BigInteger> nodeTypeTP = new HashMap<>();
            nodeTypeTP.put(Peer.Type.FOUNDATION.getCode(), BigInteger.valueOf(pocTemplate.getFoundationNodeScore())); 
            nodeTypeTP.put(Peer.Type.COMMUNITY.getCode(), BigInteger.valueOf(pocTemplate.getCommunityNodeScore())); 
            nodeTypeTP.put(Peer.Type.HUB.getCode(), BigInteger.valueOf(pocTemplate.getHubNodeScore()));
            nodeTypeTP.put(Peer.Type.BOX.getCode(), BigInteger.valueOf(pocTemplate.getBoxNodeScore())); 
            nodeTypeTP.put(Peer.Type.NORMAL.getCode(), BigInteger.valueOf(pocTemplate.getNormalNodeScore())); 

            Map<Long, BigInteger> serverOpenTP = new HashMap<>();
            serverOpenTP.put(Peer.Service.MINER.getCode(),BigInteger.valueOf(pocTemplate.getMinerScore())); 
            serverOpenTP.put(Peer.Service.BAPI.getCode(),BigInteger.valueOf(pocTemplate.getBapiScore())); 
            serverOpenTP.put(Peer.Service.NATER.getCode(),BigInteger.valueOf(pocTemplate.getNaterScore())); 
            serverOpenTP.put(Peer.Service.STORAGE.getCode(),BigInteger.valueOf(pocTemplate.getStorageScore())); 
            serverOpenTP.put(Peer.Service.PROVER.getCode(),BigInteger.valueOf(pocTemplate.getProverScore())); 

            Map<Integer, BigInteger> hardwareConfigTP = new HashMap<>();
            hardwareConfigTP.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(pocTemplate.getBadHardwareScore())); 
            hardwareConfigTP.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(pocTemplate.getMiddleHardwareScore())); 
            hardwareConfigTP.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(pocTemplate.getGoodHardwareScore())); 

            Map<Integer, BigInteger> networkConfigTP = new HashMap<>();
            networkConfigTP.put(DeviceLevels.POOR.getLevel(), BigInteger.valueOf(pocTemplate.getPoorNetworkScore())); 
            networkConfigTP.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(pocTemplate.getBadNetworkScore())); 
            networkConfigTP.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(pocTemplate.getMiddleNetworkScore())); 
            networkConfigTP.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(pocTemplate.getGoodNetworkScore())); 

            Map<Integer, BigInteger> txPerformanceTP = new HashMap<>();
            txPerformanceTP.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(pocTemplate.getBadTxScore())); 
            txPerformanceTP.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(pocTemplate.getMiddleTxScore())); 
            txPerformanceTP.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(pocTemplate.getGoodTxScore()));  

            Map<Integer, BigInteger> onlineRateFoundationTP = new HashMap<>();
            onlineRateFoundationTP.put(OnlineStatusDef.FROM_99_00_TO_99_99.getValue(), BigInteger.valueOf(pocTemplate.getFoundationFrom9900To9999())); 
            onlineRateFoundationTP.put(OnlineStatusDef.FROM_97_00_TO_99_00.getValue(), BigInteger.valueOf(pocTemplate.getFoundationFrom9700To9900())); 
            onlineRateFoundationTP.put(OnlineStatusDef.FROM_00_00_TO_97_00.getValue(), BigInteger.valueOf(pocTemplate.getFoundationFrom0000To9700())); 

            Map<Integer, BigInteger> onlineRateCommunityTP = new HashMap<>();
            onlineRateCommunityTP.put(OnlineStatusDef.FROM_97_00_TO_99_00.getValue(),BigInteger.valueOf(pocTemplate.getCommunityFrom9700To9900())); 
            onlineRateCommunityTP.put(OnlineStatusDef.FROM_90_00_TO_97_00.getValue(),BigInteger.valueOf(pocTemplate.getCommunityFrom9000To9700())); 
            onlineRateCommunityTP.put(OnlineStatusDef.FROM_00_00_TO_90_00.getValue(),BigInteger.valueOf(pocTemplate.getCommunityFrom0000To9000())); 

            Map<Integer, BigInteger> onlineRateHubBoxTP = new HashMap<>();
            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_99_00_TO_100.getValue(), BigInteger.valueOf(pocTemplate.getHbFrom9900To100())); 
            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_97_00_TO_100.getValue(), BigInteger.valueOf(pocTemplate.getHbFrom9700To100())); 
            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_00_00_TO_90_00.getValue(), BigInteger.valueOf(pocTemplate.getHbFrom0000To9000())); 

            Map<Integer, BigInteger> onlineRateNormalTP = new HashMap<>();
            onlineRateNormalTP.put(OnlineStatusDef.FROM_97_00_TO_100.getValue(), BigInteger.valueOf(pocTemplate.getNormalFrom9700To100())); 
            onlineRateNormalTP.put(OnlineStatusDef.FROM_90_00_TO_100.getValue(), BigInteger.valueOf(pocTemplate.getNormalFrom9000To100())); 

            Map<Peer.Type,Map<Integer, BigInteger>> onlineRateMap = new HashMap<>();
            onlineRateMap.put(Peer.Type.FOUNDATION,onlineRateFoundationTP);
            onlineRateMap.put(Peer.Type.COMMUNITY,onlineRateCommunityTP);
            onlineRateMap.put(Peer.Type.HUB,onlineRateHubBoxTP);
            onlineRateMap.put(Peer.Type.BOX,onlineRateHubBoxTP);
            onlineRateMap.put(Peer.Type.NORMAL,onlineRateNormalTP);

            Map<Integer, BigInteger> blockingMissTemplate = new HashMap<>();
            blockingMissTemplate.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(pocTemplate.getBadBlockingMissScore())); 
            blockingMissTemplate.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(pocTemplate.getMiddleBlockingMissScore())); 
            blockingMissTemplate.put(DeviceLevels.GOOD.getLevel(), BigInteger.valueOf(pocTemplate.getGoodBlockingMissScore())); 

            Map<Integer, BigInteger> bocSpeedTemplate = new HashMap<>();
            bocSpeedTemplate.put(DeviceLevels.POOR.getLevel(), BigInteger.valueOf(pocTemplate.getPoorBocSpeedScore())); 
            bocSpeedTemplate.put(DeviceLevels.BAD.getLevel(), BigInteger.valueOf(pocTemplate.getBadBocSpeedScore())); 
            bocSpeedTemplate.put(DeviceLevels.MIDDLE.getLevel(), BigInteger.valueOf(pocTemplate.getMiddleBocSpeedScore())); 

            Long weightTableVersion = pocTemplate.getVersion();

            return new PocWeightTable(weightMap,nodeTypeTP,serverOpenTP,hardwareConfigTP,networkConfigTP,txPerformanceTP,onlineRateMap,blockingMissTemplate,bocSpeedTemplate,weightTableVersion);
        }

        /**
         *
         * @author : yyunsen
         * @date : 2019/1/8 20:40

         * @return : org.conch.consensus.poc.tx.PocTxBody.PocWeightTable 默认PoC权重表
         */
        public static PocWeightTable defaultPocWeightTable(){

            return pocWeightTableBuilder(new PocTemplate());
        }
        
        // tx version must be set to 1
        public PocWeightTable(Map<String, BigInteger> weightMap, Map<Integer, BigInteger> nodeTypeTP, Map<Long, BigInteger> serverOpenTP, Map<Integer, BigInteger> hardwareConfigTP, 
                              Map<Integer, BigInteger> networkConfigTP, Map<Integer, BigInteger> txPerformanceTP, Map<Peer.Type, Map<Integer, BigInteger>> onlineRateTP, 
                              Map<Integer, BigInteger> generationMissTP, Map<Integer, BigInteger> bocSpeedTP, Long weightTableVersion) {
            super(1);
            this.weightMap = weightMap;
            this.nodeTypeTemplate = nodeTypeTP;
            this.serverOpenTemplate = serverOpenTP;
            this.hardwareConfigTemplate = hardwareConfigTP;
            this.networkConfigTemplate = networkConfigTP;
            this.txPerformanceTemplate = txPerformanceTP;
            this.onlineRateTemplate = onlineRateTP;
            this.generationMissingTemplate = generationMissTP;
            this.bocSpeedTemplate = bocSpeedTP;
            this.weightTableVersion = weightTableVersion;
        }

        public PocWeightTable(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.weightTableVersion = buffer.getLong();
            this.weightMap = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.nodeTypeTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.serverOpenTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.hardwareConfigTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.networkConfigTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.txPerformanceTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.onlineRateTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.generationMissingTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
            this.bocSpeedTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),Map.class);
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
            generationMissingTemplate = (Map<Integer, BigInteger>) attachmentData.get("generationMissingTemplate");
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
                    + Convert.countJsonBytes(generationMissingTemplate)
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
            Convert.writeMap(buffer, generationMissingTemplate);
            Convert.writeMap(buffer,bocSpeedTemplate);
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
            attachment.put("generationMissingTemplate", generationMissingTemplate);
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
        // 网络在线率百分比的值乘以 100，用 int 表示, 例 99% = 9900， 99.99% = 9999
        private final int networkRate;

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

    final class PocGenerationMissing extends Attachment.TxBodyBase {
        private List<Long> missingAccountIds;
        private int missingTimeStamp;

        public PocGenerationMissing(List<Long> missingAccountIds) {
            this.missingAccountIds = missingAccountIds;
            this.missingTimeStamp = Conch.getEpochTime();
        }

        public List<Long> getMissingAccountIds() {
            return missingAccountIds;
        }

        public int getMissingTimeStamp() {
            return missingTimeStamp;
        }

        public PocGenerationMissing(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
             super(buffer, transactionVersion);
             this.missingAccountIds = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer,buffer.getInt(),MAX_POC_ITEM_BYTEBUFFER),List.class);
             this.missingTimeStamp = buffer.getInt();
        }

        public PocGenerationMissing(JSONObject attachmentData) {
            super(attachmentData);
            this.missingAccountIds = (List<Long>) attachmentData.get("missingAccountIds");
            this.missingTimeStamp = (int) attachmentData.get("missingTimeStamp");
        }

        @Override
        public int getMySize() {
            return Convert.countJsonBytes(missingAccountIds) + 4;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            Convert.writeList(buffer, missingAccountIds);
            buffer.putInt(missingTimeStamp);
        }

        @Override
        public void putMyJSON(JSONObject json) {
              json.put("missingAccountIds", missingAccountIds);
              json.put("missingTimeStamp", missingTimeStamp);
        }

        @Override
        public TransactionType getTransactionType() {
            return PocTxWrapper.POC_BLOCK_MISSING;
        }
    }

    /**
     * Bifurcation of convergence for PoC
     */
    final class PocBcSpeed extends Attachment.TxBodyBase {
        private final String ip;
        private final String port;
        // 分叉收敛速度 1-硬分叉；2-慢；3-中；4-快
        private final int speed;

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
