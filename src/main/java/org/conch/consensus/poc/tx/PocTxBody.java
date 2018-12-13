package org.conch.consensus.poc.tx;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.mint.pool.PoolRule;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.TransactionType;
import org.conch.util.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public abstract class PocTxBody  {

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
        private Map<String, Object> scoreMap;
        private Map<String, Object> weightMap;

        public Map<String, Object> getScoreMap() {
            return scoreMap;
        }

        public Map<String, Object> getWeightMap() {
            return weightMap;
        }

        public PocWeightTable(Map<String, Object> scoreMap, Map<String, Object> weightMap) {
            this.scoreMap = scoreMap;
            this.weightMap = weightMap;
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
                weightMap = (Map<String,Object>)ois.readObject();
                ois.close();
            }catch (Exception e){
                Logger.logErrorMessage("sharder pool create transaction can't load rule from byte", e);
            }
            this.scoreMap = scoreMap;
            this.weightMap = weightMap;
        }

        public PocWeightTable(JSONObject attachmentData) {
            super(attachmentData);
            this.scoreMap = PoolRule.jsonObjectToMap((JSONObject) attachmentData.get("score"));
            this.weightMap = PoolRule.jsonObjectToMap((JSONObject) attachmentData.get("weight"));
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
            try{
                ByteArrayOutputStream boScore = new ByteArrayOutputStream();
                ObjectOutputStream osScore = new ObjectOutputStream(boScore);
                osScore.writeObject(scoreMap);
                osScore.close();

                ByteArrayOutputStream boWeight = new ByteArrayOutputStream();
                ObjectOutputStream osWeight = new ObjectOutputStream(boWeight);
                osWeight.writeObject(weightMap);
                osWeight.close();
                return boWeight.toByteArray().length + boScore.toByteArray().length;
            }catch (Exception e){
                Logger.logDebugMessage("rule can't turn to byte in sharder poc weight table", e);
            }
            return (int) (ObjectSizeCalculator.getObjectSize(scoreMap) + ObjectSizeCalculator.getObjectSize(weightMap));
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            try{
                ByteArrayOutputStream boScore = new ByteArrayOutputStream();
                ObjectOutputStream osScore = new ObjectOutputStream(boScore);
                osScore.writeObject(scoreMap);
                osScore.close();

                ByteArrayOutputStream boWeight = new ByteArrayOutputStream();
                ObjectOutputStream osWeight = new ObjectOutputStream(boWeight);
                osWeight.writeObject(weightMap);
                osWeight.close();

                buffer.put(ByteBuffer.wrap(boScore.toByteArray()));
                buffer.put(ByteBuffer.wrap(boWeight.toByteArray()));
            }catch (Exception e){
                Logger.logDebugMessage("rule can't turn to byte in sharder poc weight table", e);
            }
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("weight", weightMap);
            attachment.put("score", scoreMap);
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
