package org.conch.consensus.poc.hardware;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * 系统信息对象
 * @author 栗子
 */
public class SystemInfo {

    /**
     * API用到的节点ip
     */
    private String ip;

    /**
     * API用到的节点端口
     */
    private String port;

    /**
     * 性能测试用，穿透服务地址或本机地址
     */
    private String address;

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
    private int memoryTotal;

    /**
     * 硬盘大小 （单位GB）
     */
    private int hardDiskSize;

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

    public String getAddress() {
        return address;
    }

    public SystemInfo setAddress(String address) {
        this.address = address;
        return this;
    }

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

    public int getMemoryTotal() {
        return memoryTotal;
    }

    public SystemInfo setMemoryTotal(int memoryTotal) {
        this.memoryTotal = memoryTotal;
        return this;
    }

    public int getHardDiskSize() {
        return hardDiskSize;
    }

    public SystemInfo setHardDiskSize(int hardDiskSize) {
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
