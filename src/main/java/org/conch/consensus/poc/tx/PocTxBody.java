package org.conch.consensus.poc.tx;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocTemplate;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.TransactionType;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public interface PocTxBody {
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
    
    class PocNodeTypeV3 extends PocNodeTypeV2 {
        // unit is KB
        private long diskCapacity;

        public long getDiskCapacity() {
            return diskCapacity;
        }

        public PocNodeTypeV3(String ip, Peer.Type type, long accountId, long diskCapacity) {
            super(ip, type, accountId);
            this.diskCapacity = diskCapacity;
        }

        public PocNodeTypeV3(PocNodeTypeV2 typeV2, long diskCapacity) {
            super(typeV2.ip, typeV2.type, typeV2.accountId);
            this.diskCapacity = diskCapacity;
        }

        public PocNodeTypeV3(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.diskCapacity = buffer.getLong();
        }

        public PocNodeTypeV3(JSONObject attachmentData) {
            super(attachmentData);
            this.diskCapacity = (Long) attachmentData.get("diskCapacity");
        }

        @Override
        public int getMySize() {
            return super.getMySize() + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.putLong(diskCapacity);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("diskCapacity", this.diskCapacity);
        }
    }

    /**
     * use the PocNodeTypeV3: org.conch.consensus.poc.tx.PocTxBody.PocNodeTypeV3
     */
    class PocNodeTypeV2 extends PocNodeType {
        protected long accountId;

        public long getAccountId() {
            return accountId;
        }

        public PocNodeTypeV2(String ip, Peer.Type type, long accountId) {
            super(ip, type);
            this.accountId = accountId;
        }


        public PocNodeTypeV2(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.accountId = buffer.getLong();
        }

        public PocNodeTypeV2(JSONObject attachmentData) {
            super(attachmentData);
            this.accountId = (Long) attachmentData.get("accountId");
        }

        @Override
        public int getMySize() {
            return super.getMySize() + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.putLong(accountId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("accountId", this.accountId);
        }
    }

    /**
     * use the PocNodeTypeV3: org.conch.consensus.poc.tx.PocTxBody.PocNodeTypeV3
     */
    class PocNodeType extends Attachment.TxBodyBase {
        /**
         * TODO need refactor
         * ip = socket address => host:port (my address)
         * IP Socket Address (IP address + port number) It can also be a pair (hostname + port number)
         */
        protected String ip;
        protected Peer.Type type;
       

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
                this.ip = Convert.readString(buffer, buffer.getShort(), 10);
            } catch (ConchException.NotValidException e) {
                Logger.logErrorMessage("Can't parse the ip of the PocNodeType caused by: " + e.getMessage());
                this.ip = "";
            }
        }

        public PocNodeType(JSONObject attachmentData) {
            super(attachmentData);
            this.ip = (String) attachmentData.get("ip");
            Object obj = attachmentData.get("type");
            Integer code = obj instanceof Long ? ((Long) obj).intValue() : (Integer)obj;
            this.type = Peer.Type.getByCode(code);
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

    final class PocWeightTable extends Attachment.TxBodyBase implements Serializable {

        private Map<String, Integer> weightMap;
        private Map<Integer, Integer> nodeTypeTemplate;
        private Map<Long, Integer> serverOpenTemplate;
        private Map<Integer, Integer> hardwareConfigTemplate;
        private Map<Integer, Integer> networkConfigTemplate;
        private Map<Integer, Integer> txPerformanceTemplate;

        private Map<Peer.Type, Map<Integer, Integer>> onlineRateTemplate;

        private Map<Integer, Integer> generationMissingTemplate;
        private Map<Integer, Integer> bocSpeedTemplate;

        private Long weightTableVersion;

        public Map<String, Integer> getWeightMap() {
            return weightMap;
        }

        public Map<Integer, Integer> getNodeTypeTemplate() {
            return nodeTypeTemplate;
        }

        public Map<Long, Integer> getServerOpenTemplate() {
            return serverOpenTemplate;
        }

        public Map<Integer, Integer> getHardwareConfigTemplate() {
            return hardwareConfigTemplate;
        }

        public Map<Integer, Integer> getNetworkConfigTemplate() {
            return networkConfigTemplate;
        }

        public Map<Integer, Integer> getTxPerformanceTemplate() {
            return txPerformanceTemplate;
        }

        public Map<Peer.Type, Map<Integer, Integer>> getOnlineRateTemplate() {
            return onlineRateTemplate;
        }

        public void setOnlineRateTemplate(Map<Peer.Type, Map<Integer, Integer>> onlineRateTemplate) {
            this.onlineRateTemplate = onlineRateTemplate;
        }

        public Map<Integer, Integer> getOnlineRateTemplate(Peer.Type type) {
            if (type == null || onlineRateTemplate == null || onlineRateTemplate.size() <= 0) {
                return null;
            }
            return onlineRateTemplate.get(type);
        }

        public Map<Integer, Integer> getGenerationMissingTemplate() {
            return generationMissingTemplate;
        }

        public Map<Integer, Integer> getBocSpeedTemplate() {
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
         *
         * @param pocTemplate : 默认模板或自定义模板
         * @return : org.conch.consensus.poc.tx.PocTxBody.PocWeightTable 权重表
         * @author : yyunsen
         * @date : 2019/1/8 20:41
         */
        public static PocWeightTable pocWeightTableBuilder(PocTemplate pocTemplate) {
            Map<String, Integer> weightMap = new HashMap<>();
            weightMap.put(WeightTableOptions.NODE_TYPE.value, pocTemplate.getNodeTypeWeight().intValue());
            //weightMap.put(WeightTableOptions.SERVER_OPEN.value, pocTemplate.getServerOpenWeight().intValue());
            weightMap.put(WeightTableOptions.SS_HOLD.value, pocTemplate.getSsHoldWeight().intValue());
            weightMap.put(WeightTableOptions.HARDWARE_CONFIG.value, pocTemplate.getHardwareConfWeight().intValue());
            weightMap.put(WeightTableOptions.NETWORK_CONFIG.value, pocTemplate.getNetWorkConfWeight().intValue());
            weightMap.put(WeightTableOptions.TX_PERFORMANCE.value, pocTemplate.getTxHandlePerformanceWeight().intValue());

            Map<Integer, Integer> nodeTypeTP = new HashMap<>();
            nodeTypeTP.put(Peer.Type.FOUNDATION.getCode(), pocTemplate.getFoundationNodeScore().intValue());
            //nodeTypeTP.put(Peer.Type.COMMUNITY.getCode(), pocTemplate.getCommunityNodeScore().intValue());
            nodeTypeTP.put(Peer.Type.SOUL.getCode(), pocTemplate.getHubNodeScore().intValue());
            nodeTypeTP.put(Peer.Type.CENTER.getCode(), pocTemplate.getBoxNodeScore().intValue());
            nodeTypeTP.put(Peer.Type.NORMAL.getCode(), pocTemplate.getNormalNodeScore().intValue());

            Map<Long, Integer> serverOpenTP = new HashMap<>();
            serverOpenTP.put(Peer.Service.MINER.getCode(), pocTemplate.getMinerScore().intValue());
            serverOpenTP.put(Peer.Service.BAPI.getCode(), pocTemplate.getBapiScore().intValue());
            serverOpenTP.put(Peer.Service.NATER.getCode(), pocTemplate.getNaterScore().intValue());
            serverOpenTP.put(Peer.Service.STORAGE.getCode(), pocTemplate.getStorageScore().intValue());
            serverOpenTP.put(Peer.Service.PROVER.getCode(), pocTemplate.getProverScore().intValue());

            Map<Integer, Integer> hardwareConfigTP = new HashMap<>();
            hardwareConfigTP.put(DeviceLevels.BAD.getLevel(), pocTemplate.getBadHardwareScore().intValue());
            hardwareConfigTP.put(DeviceLevels.MIDDLE.getLevel(), pocTemplate.getMiddleHardwareScore().intValue());
            hardwareConfigTP.put(DeviceLevels.GOOD.getLevel(), pocTemplate.getGoodHardwareScore().intValue());

            Map<Integer, Integer> networkConfigTP = new HashMap<>();
            networkConfigTP.put(DeviceLevels.POOR.getLevel(), pocTemplate.getPoorNetworkScore().intValue());
            networkConfigTP.put(DeviceLevels.BAD.getLevel(), pocTemplate.getBadNetworkScore().intValue());
            networkConfigTP.put(DeviceLevels.MIDDLE.getLevel(), pocTemplate.getMiddleNetworkScore().intValue());
            networkConfigTP.put(DeviceLevels.GOOD.getLevel(), pocTemplate.getGoodNetworkScore().intValue());

            Map<Integer, Integer> txPerformanceTP = new HashMap<>();
            txPerformanceTP.put(DeviceLevels.BAD.getLevel(), pocTemplate.getBadTxScore().intValue());
            txPerformanceTP.put(DeviceLevels.MIDDLE.getLevel(), pocTemplate.getMiddleTxScore().intValue());
            txPerformanceTP.put(DeviceLevels.GOOD.getLevel(), pocTemplate.getGoodTxScore().intValue());

//            Map<Integer, Integer> onlineRateFoundationTP = new HashMap<>();
//            onlineRateFoundationTP.put(OnlineStatusDef.FROM_99_00_TO_99_99.getValue(), pocTemplate.getFoundationFrom9900To9999().intValue());
//            onlineRateFoundationTP.put(OnlineStatusDef.FROM_97_00_TO_99_00.getValue(), pocTemplate.getFoundationFrom9700To9900().intValue());
//            onlineRateFoundationTP.put(OnlineStatusDef.FROM_00_00_TO_97_00.getValue(), pocTemplate.getFoundationFrom0000To9700().intValue());
//
//            Map<Integer, Integer> onlineRateCommunityTP = new HashMap<>();
//            onlineRateCommunityTP.put(OnlineStatusDef.FROM_97_00_TO_99_00.getValue(), pocTemplate.getCommunityFrom9700To9900().intValue());
//            onlineRateCommunityTP.put(OnlineStatusDef.FROM_90_00_TO_97_00.getValue(), pocTemplate.getCommunityFrom9000To9700().intValue());
//            onlineRateCommunityTP.put(OnlineStatusDef.FROM_00_00_TO_90_00.getValue(), pocTemplate.getCommunityFrom0000To9000().intValue());
//
//            Map<Integer, Integer> onlineRateHubBoxTP = new HashMap<>();
//            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_99_00_TO_100.getValue(), pocTemplate.getHbFrom9900To100().intValue());
//            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_97_00_TO_100.getValue(), pocTemplate.getHbFrom9700To100().intValue());
//            onlineRateHubBoxTP.put(OnlineStatusDef.FROM_00_00_TO_90_00.getValue(), pocTemplate.getHbFrom0000To9000().intValue());
//
//            Map<Integer, Integer> onlineRateNormalTP = new HashMap<>();
//            onlineRateNormalTP.put(OnlineStatusDef.FROM_97_00_TO_100.getValue(), pocTemplate.getNormalFrom9700To100().intValue());
//            onlineRateNormalTP.put(OnlineStatusDef.FROM_90_00_TO_100.getValue(), pocTemplate.getNormalFrom9000To100().intValue());

            Map<Integer, Integer> onlineRateTP = new HashMap<>();
            onlineRateTP.put(OnlineStatusDef.FROM_99_00_TO_99_99.getValue(), pocTemplate.getFoundationFrom9900To9999().intValue());
            onlineRateTP.put(OnlineStatusDef.FROM_97_00_TO_99_00.getValue(), pocTemplate.getFoundationFrom9700To9900().intValue());
            onlineRateTP.put(OnlineStatusDef.FROM_00_00_TO_97_00.getValue(), pocTemplate.getFoundationFrom0000To9700().intValue());

            Map<Peer.Type, Map<Integer, Integer>> onlineRateMap = new HashMap<>();
            onlineRateMap.put(Peer.Type.FOUNDATION, Maps.newHashMap());
            onlineRateMap.put(Peer.Type.COMMUNITY, Maps.newHashMap());
            onlineRateMap.put(Peer.Type.SOUL, Maps.newHashMap());
            onlineRateMap.put(Peer.Type.CENTER, Maps.newHashMap());
            onlineRateMap.put(Peer.Type.NORMAL, Maps.newHashMap());

            Map<Integer, Integer> blockingMissTemplate = new HashMap<>();
            blockingMissTemplate.put(DeviceLevels.BAD.getLevel(), pocTemplate.getBadBlockingMissScore().intValue());
            blockingMissTemplate.put(DeviceLevels.MIDDLE.getLevel(), pocTemplate.getMiddleBlockingMissScore().intValue());
            blockingMissTemplate.put(DeviceLevels.GOOD.getLevel(), pocTemplate.getGoodBlockingMissScore().intValue());

            Map<Integer, Integer> bocSpeedTemplate = new HashMap<>();
            bocSpeedTemplate.put(DeviceLevels.POOR.getLevel(), pocTemplate.getPoorBocSpeedScore().intValue());
            bocSpeedTemplate.put(DeviceLevels.BAD.getLevel(), pocTemplate.getBadBocSpeedScore().intValue());
            bocSpeedTemplate.put(DeviceLevels.MIDDLE.getLevel(), pocTemplate.getMiddleBocSpeedScore().intValue());

            Long weightTableVersion = pocTemplate.getVersion();
            return new PocWeightTable(weightMap, nodeTypeTP, serverOpenTP, hardwareConfigTP, networkConfigTP, txPerformanceTP, onlineRateMap, blockingMissTemplate, bocSpeedTemplate, weightTableVersion);
        }

        /**
         * @return : org.conch.consensus.poc.tx.PocTxBody.PocWeightTable 默认PoC权重表
         * @author : yyunsen
         * @date : 2019/1/8 20:40
         */
        public static PocWeightTable defaultPocWeightTable() {
            return pocWeightTableBuilder(new PocTemplate());
        }

        // tx version must be set to 1
        public PocWeightTable(Map<String, Integer> weightMap, Map<Integer, Integer> nodeTypeTP, Map<Long, Integer> serverOpenTP, Map<Integer, Integer> hardwareConfigTP,
                              Map<Integer, Integer> networkConfigTP, Map<Integer, Integer> txPerformanceTP, Map<Peer.Type, Map<Integer, Integer>> onlineRateTP,
                              Map<Integer, Integer> generationMissTP, Map<Integer, Integer> bocSpeedTP, Long weightTableVersion) {
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
            this.weightMap = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.nodeTypeTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.serverOpenTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.hardwareConfigTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.networkConfigTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.txPerformanceTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.onlineRateTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.generationMissingTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
            this.bocSpeedTemplate = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), Map.class);
        }

        public PocWeightTable(JSONObject attachmentData) {
            super(attachmentData);
            weightTableVersion = (Long) attachmentData.get("templateVersion");
            weightMap = (Map<String, Integer>) attachmentData.get("weightMap");
            nodeTypeTemplate = (Map<Integer, Integer>) attachmentData.get("nodeTypeTemplate");
            serverOpenTemplate = (Map<Long, Integer>) attachmentData.get("serverOpenTemplate");
            hardwareConfigTemplate = (Map<Integer, Integer>) attachmentData.get("hardwareConfigTemplate");
            networkConfigTemplate = (Map<Integer, Integer>) attachmentData.get("networkConfigTemplate");
            txPerformanceTemplate = (Map<Integer, Integer>) attachmentData.get("txPerformanceTemplate");
            onlineRateTemplate = (Map<Peer.Type, Map<Integer, Integer>>) attachmentData.get("onlineRateTemplate");
            generationMissingTemplate = (Map<Integer, Integer>) attachmentData.get("generationMissingTemplate");
            bocSpeedTemplate = (Map<Integer, Integer>) attachmentData.get("bocSpeedTemplate");

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
            Convert.writeMap(buffer, weightMap);
            Convert.writeMap(buffer, nodeTypeTemplate);
            Convert.writeMap(buffer, serverOpenTemplate);
            Convert.writeMap(buffer, hardwareConfigTemplate);
            Convert.writeMap(buffer, networkConfigTemplate);
            Convert.writeMap(buffer, txPerformanceTemplate);
            Convert.writeMap(buffer, onlineRateTemplate);
            Convert.writeMap(buffer, generationMissingTemplate);
            Convert.writeMap(buffer, bocSpeedTemplate);
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

        private String ip;
        private String port;
        protected long accountId;
        private SystemInfo systemInfo;

        public String getIp() {
            return ip;
        }

        public String getPort() {
            return port;
        }

        public long getAccountId() {
            return accountId;
        }

        public SystemInfo getSystemInfo() {
            return systemInfo;
        }

        public PocNodeConf(String ip, String port, SystemInfo systemInfo, long accountId) {
            this(ip, port, systemInfo);
            this.accountId = accountId;
        }

        public PocNodeConf(String ip, String port, SystemInfo systemInfo) {
            this.ip = ip;
            this.port = port;
            this.systemInfo = systemInfo;
        }

        public PocNodeConf(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            try {
                this.ip = Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER);
                this.port = Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER);
                this.systemInfo = JSON.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), SystemInfo.class);
                this.accountId = buffer.getLong();
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        }

        public PocNodeConf(JSONObject attachmentData) {
            super(attachmentData);
            this.ip = (String) attachmentData.get("ip");
            this.port = (String) attachmentData.get("port");
            this.systemInfo = (SystemInfo) attachmentData.get("systemInfo");
            this.accountId = (Long) attachmentData.get("accountId");
        }
        
        public String getHost(){
            return this.ip + ":" + port;
        }

        @Override
        public int getMySize() {
            return 4 * 3 + ip.getBytes().length + port.getBytes().length
                    + Convert.countJsonBytes(systemInfo) + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            Convert.writeString(buffer, ip);
            Convert.writeString(buffer, port);
            Convert.writeObject(buffer, systemInfo);
            buffer.putLong(accountId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("ip", ip);
            attachment.put("port", port);
            attachment.put("systemInfo", systemInfo);
            attachment.put("accountId", this.accountId);
        }

        @Override
        public TransactionType getTransactionType() {
            return PocTxWrapper.POC_NODE_CONF;
        }
    }

    final class PocOnlineRate extends Attachment.TxBodyBase {
        private String ip;
        private String port;
        // 网络在线率百分比的值乘以 100，用 int 表示, 例 99% = 9900， 99.99% = 9999
        private int networkRate;

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
            this.networkRate = buffer.getInt();
            try {
                this.ip = Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER);
                this.port = Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER);
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        }

        public PocOnlineRate(JSONObject attachmentData) {
            super(attachmentData);
            this.ip = (String) attachmentData.get("ip");
            this.port = (String) attachmentData.get("port");
            this.networkRate = (int) attachmentData.get("networkRate");
        }

        public String getHost(){
            return this.ip + ":" + port;
        }

        @Override
        public int getMySize() {
            return 4 + 4 * 2 + ip.getBytes().length + port.getBytes().length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(networkRate);
            Convert.writeString(buffer, ip);
            Convert.writeString(buffer, port);
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
            this.missingTimeStamp = buffer.getInt();
            this.missingAccountIds = com.alibaba.fastjson.JSONObject.parseObject(Convert.readString(buffer, buffer.getInt(), MAX_POC_ITEM_BYTEBUFFER), List.class);

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
            buffer.putInt(missingTimeStamp);
            Convert.writeList(buffer, missingAccountIds);
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
