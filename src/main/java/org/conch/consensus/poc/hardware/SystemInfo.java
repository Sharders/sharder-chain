package org.conch.consensus.poc.hardware;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * 系统信息对象
 * @author CloudSen
 */
public class SystemInfo implements Serializable {

    private static final long serialVersionUID = -3649460156403566640L;
    /**
     * 性能测试用，穿透服务地址或本机地址
     */
    private String ip;

    /**
     * API用到的节点端口
     */
    private String port;

    private String address;

    /**
     * 绑定的MW账号CDW-XXX
     */
    private String bindRs;

    /**
     * 几核
     */
    private int core;

    /**
     * 平均兆赫
     */
    private int averageMHz;

    /**
     * 内存大小 （单位GB）
     */
    private long memoryTotal;

    /**
     * 硬盘大小 （单位KB）
     */
    private long hardDiskSize;

    /**
     * 是否有公网IP
     */
    private boolean hadPublicIp;

    /**
     * 公网ip带宽（Mbps），如果没有公网ip，这个值就是0
     */
    private int bandWidth;

    /**
     * 交易处理性能
     */
    private long tradePerformance;

    /**
     * 开启的服务列表
     */
    private Long[] openServices;

    /**
     * 当前网络类型 dev,alpha,beta
     */
    private String networkType;

    /**
     * 简单节点类型
     */
    private String nodeType;

    public String getIp() {
        return ip;
    }

    public SystemInfo setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public String getPort() {
        return port;
    }

    public SystemInfo setPort(String port) {
        this.port = port;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public SystemInfo setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getBindRs() {
        return bindRs;
    }

    public SystemInfo setBindRs(String bindRs) {
        this.bindRs = bindRs;
        return this;
    }

    public int getCore() {
        return core;
    }

    public SystemInfo setCore(int core) {
        this.core = core;
        return this;
    }

    public int getAverageMHz() {
        return averageMHz;
    }

    public SystemInfo setAverageMHz(int averageMHz) {
        this.averageMHz = averageMHz;
        return this;
    }

    public long getMemoryTotal() {
        return memoryTotal;
    }

    public SystemInfo setMemoryTotal(long memoryTotal) {
        this.memoryTotal = memoryTotal;
        return this;
    }

    public long getHardDiskSizeInGB() {
        return hardDiskSize / 1024L / 1024L;
    }

    public long getHardDiskSize() {
        return hardDiskSize;
    }

    public SystemInfo setHardDiskSize(long hardDiskSize) {
        this.hardDiskSize = hardDiskSize;
        return this;
    }

    public boolean isHadPublicIp() {
        return hadPublicIp;
    }

    public SystemInfo setHadPublicIp(boolean hadPublicIp) {
        this.hadPublicIp = hadPublicIp;
        return this;
    }

    public int getBandWidth() {
        return bandWidth;
    }

    public SystemInfo setBandWidth(int bandWidth) {
        this.bandWidth = bandWidth;
        return this;
    }

    public long getTradePerformance() {
        return tradePerformance;
    }

    public SystemInfo setTradePerformance(long tradePerformance) {
        this.tradePerformance = tradePerformance;
        return this;
    }

    public Long[] getOpenServices() {
        return openServices;
    }

    public SystemInfo setOpenServices(Long[] openServices) {
        this.openServices = openServices;
        return this;
    }

    public String getNetworkType() {
        return networkType;
    }

    public SystemInfo setNetworkType(String networkType) {
        this.networkType = networkType;
        return this;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(this.toJsonString());
    }

    public String toJsonString() {
        return JSONObject.toJSONString(this);
    }
}
