package org.conch.consensus.poc.tx;

import org.conch.consensus.poc.hardware.DeviceInfo;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.tx.Attachment;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public abstract class PocTxBody  {

    private PocTxBody() {}
    
    public final class PocWeightTable extends Attachment.TxBodyBase {
        private final String ip;
        private final String port;
        private final BigInteger nodeWeight; // 节点类型 加权后的值
        private final BigInteger serverWeight; // 开启服务 加权后的值
        private final BigInteger configWeight; // 硬件配置 加权后的值
        private final BigInteger networkWeight; // 网络配置 加权后的值
        private final BigInteger tpWeight; // TransactionProcessing Perfonnallce 交易处理性能 加权后的值
        private final BigInteger ssHoldWeight; // SS持有量 加权后的值
        private final BigInteger blockingMissWeight; // 出块丢失 加权后的值
        private final BigInteger bifuractionConvergenceWeight; // 分叉收敛 加权后的值
        private final BigInteger onlineRateWeight; // 在线率 加权后的值
    
        public String getIp() {
          return ip;
        }
    
        public String getPort() {
          return port;
        }
    
        public BigInteger getNodeWeight() {
          return nodeWeight;
        }
    
        public BigInteger getServerWeight() {
          return serverWeight;
        }
    
        public BigInteger getConfigWeight() {
          return configWeight;
        }
    
        public BigInteger getNetworkWeight() {
          return networkWeight;
        }
    
        public BigInteger getTpWeight() {
          return tpWeight;
        }
    
        public BigInteger getSsHoldWeight() {
          return ssHoldWeight;
        }
    
        public BigInteger getBlockingMissWeight() {
          return blockingMissWeight;
        }
    
        public BigInteger getBifuractionConvergenceWeight() {
          return bifuractionConvergenceWeight;
        }
    
        public BigInteger getOnlineRateWeight() {
          return onlineRateWeight;
        }
        
        public PocWeightTable(
            String ip,
            String port,
            BigInteger nodeWeight,
            BigInteger serverWeight,
            BigInteger configWeight,
            BigInteger networkWeight,
            BigInteger tpWeight,
            BigInteger ssHoldWeight,
            BigInteger blockingMissWeight,
            BigInteger bifuractionConvergenceWeight,
            BigInteger onlineRateWeight) {
          this.ip = ip;
          this.port = port;
          this.nodeWeight = nodeWeight;
          this.serverWeight = serverWeight;
          this.configWeight = configWeight;
          this.networkWeight = networkWeight;
          this.tpWeight = tpWeight;
          this.ssHoldWeight = ssHoldWeight;
          this.blockingMissWeight = blockingMissWeight;
          this.bifuractionConvergenceWeight = bifuractionConvergenceWeight;
          this.onlineRateWeight = onlineRateWeight;
        }


        public PocWeightTable(ByteBuffer buffer, byte transactionVersion) {
          super(buffer, transactionVersion);
          this.ip = buffer.toString();
          this.port = buffer.toString();
          this.nodeWeight = new BigInteger(buffer.array());
          this.serverWeight = new BigInteger(buffer.array());
          this.configWeight = new BigInteger(buffer.array());
          this.networkWeight = new BigInteger(buffer.array());
          this.tpWeight = new BigInteger(buffer.array());
          this.ssHoldWeight = new BigInteger(buffer.array());
          this.blockingMissWeight = new BigInteger(buffer.array());
          this.bifuractionConvergenceWeight = new BigInteger(buffer.array());
          this.onlineRateWeight = new BigInteger(buffer.array());
        }
    
        public PocWeightTable(JSONObject attachmentData) {
          super(attachmentData);
          this.ip = (String) attachmentData.get("ip");
          this.port = (String) attachmentData.get("port");
          this.nodeWeight = (BigInteger) attachmentData.get("nodeWeight");
          this.serverWeight = (BigInteger) attachmentData.get("serverWeight");
          this.configWeight = (BigInteger) attachmentData.get("configWeight");
          this.networkWeight = (BigInteger) attachmentData.get("networkWeight");
          this.tpWeight = (BigInteger) attachmentData.get("tpWeight");
          this.ssHoldWeight = (BigInteger) attachmentData.get("ssHoldWeight");
          this.blockingMissWeight = (BigInteger) attachmentData.get("blockingMissWeight");
          this.bifuractionConvergenceWeight =
              (BigInteger) attachmentData.get("bifuractionConvergenceWeight");
          this.onlineRateWeight = (BigInteger) attachmentData.get("onlineRateWeight");
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
          return ip.getBytes().length
              + port.getBytes().length
              + nodeWeight.toByteArray().length
              + serverWeight.toByteArray().length
              + configWeight.toByteArray().length
              + networkWeight.toByteArray().length
              + tpWeight.toByteArray().length
              + ssHoldWeight.toByteArray().length
              + blockingMissWeight.toByteArray().length
              + bifuractionConvergenceWeight.toByteArray().length
              + onlineRateWeight.toByteArray().length;
        }
    
        @Override
        public void putMyBytes(ByteBuffer buffer) {
          buffer.put(ip.getBytes());
          buffer.put(port.getBytes());
          buffer.put(nodeWeight.toByteArray());
          buffer.put(serverWeight.toByteArray());
          buffer.put(configWeight.toByteArray());
          buffer.put(networkWeight.toByteArray());
          buffer.put(tpWeight.toByteArray());
          buffer.put(ssHoldWeight.toByteArray());
          buffer.put(blockingMissWeight.toByteArray());
          buffer.put(bifuractionConvergenceWeight.toByteArray());
          buffer.put(onlineRateWeight.toByteArray());
        }
    
        @Override
        public void putMyJSON(JSONObject attachment) {
          attachment.put("ip", ip);
          attachment.put("port", port);
          attachment.put("nodeWeight", nodeWeight);
          attachment.put("serverWeight", serverWeight);
          attachment.put("configWeight", configWeight);
          attachment.put("networkWeight", networkWeight);
          attachment.put("tpWeight", tpWeight);
          attachment.put("ssHoldWeight", ssHoldWeight);
          attachment.put("blockingMissWeight", blockingMissWeight);
          attachment.put("bifuractionConvergenceWeight", bifuractionConvergenceWeight);
          attachment.put("onlineRateWeight", onlineRateWeight);
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
    private DeviceInfo deviceInfo;

    public String getIp() {
      return ip;
    }

    public String getPort() {
      return port;
    }

    public SystemInfo getSystemInfo() {
      return systemInfo;
    }

    public DeviceInfo getDeviceInfo() {
      return deviceInfo;
    }

    public PocNodeConf(String ip, String port, SystemInfo systemInfo, DeviceInfo deviceInfo) {
      this.ip = ip;
      this.port = port;
      this.systemInfo = systemInfo;
      this.deviceInfo = deviceInfo;
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
        if (obj instanceof DeviceInfo) {
          this.deviceInfo = (DeviceInfo) obj;
        } else {
          this.deviceInfo = null;
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
      this.deviceInfo = (DeviceInfo) attachmentData.get("deviceInfo");
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
          + _readByteSize(systemInfo)
          + _readByteSize(deviceInfo);
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
      buffer.put(ip.getBytes());
      buffer.put(port.getBytes());

      _putByteSize(buffer, systemInfo);
      _putByteSize(buffer, deviceInfo);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
      attachment.put("ip", ip);
      attachment.put("port", port);
      attachment.put("systemInfo", systemInfo);
      attachment.put("deviceInfo", deviceInfo);
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
    private final String ip;
    private final String port;
    private final int missLevel; // 0-零丢失； 1-低；2-中；3-高

    public String getIp() {
      return ip;
    }

    public String getPort() {
      return port;
    }

    public int getMissLevel() {
      return missLevel;
    }

    public PocBlockMiss(String ip, String port, int missLevel) {
      this.ip = ip;
      this.port = port;
      this.missLevel = missLevel;
    }

    public PocBlockMiss(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.ip = buffer.toString();
      this.port = buffer.toString();
      this.missLevel = buffer.getInt();
    }

    public PocBlockMiss(JSONObject attachmentData) {
      super(attachmentData);
      this.ip = (String) attachmentData.get("ip");
      this.port = (String) attachmentData.get("port");
      this.missLevel = (int) attachmentData.get("missLevel");
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
      return 2 + ip.getBytes().length + port.getBytes().length;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
      buffer.put(ip.getBytes());
      buffer.put(port.getBytes());
      buffer.putInt(missLevel);
    }

    @Override
    public void putMyJSON(JSONObject json) {
      json.put("ip", ip);
      json.put("port", port);
      json.put("missCount", missLevel);
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
