package org.conch.consensus.poc.hardware;

import java.io.Serializable;

/**********************************************************************************
 * @package org.conch.consensus.poc.hardware
 * @author Wolf Tian
 * @email twenbin@sharder.org
 * @company Sharder Foundation
 * @website https://www.sharder.org/
 * @creatAt 2018-Dec-05 15:20 Wed
 * @tel 18716387615
 * @comment
 **********************************************************************************/
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 6673192742052098044L;

    private int type; // 节点类型 (1-基金会节点; 2-社区节点; 3-Hub节点; 4-Box节点; 5-普通节点)
    private boolean serverOpen; // 服务是否开启
    private int tradePerformance; // 交易处理性能
    private boolean hadPublicIp; // 是否有公网IP
    private int bandWidth; // 公网ip带宽（Mbps），如果没有公网ip，这个值就是0

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isServerOpen() {
        return serverOpen;
    }

    public void setServerOpen(boolean serverOpen) {
        this.serverOpen = serverOpen;
    }

    public int getTradePerformance() {
        return tradePerformance;
    }

    public void setTradePerformance(int tradePerformance) {
        this.tradePerformance = tradePerformance;
    }

    public boolean getHadPublicIp() {
        return hadPublicIp;
    }

    public void setHadPublicIp(boolean hadPublicIp) {
        this.hadPublicIp = hadPublicIp;
    }

    public int getBandWidth() {
        return bandWidth;
    }

    public void setBandWidth(int bandWidth) {
        this.bandWidth = bandWidth;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" + "type=" + type + ", serverOpen=" + serverOpen + ", tradePerformance=" + tradePerformance + ", hadPublicIp=" + hadPublicIp + ", bandWidth=" + bandWidth + '}';
    }
}
