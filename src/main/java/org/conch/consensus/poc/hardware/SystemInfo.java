package org.conch.consensus.poc.hardware;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @ClassName SystemInfo
 * @Description  系统信息对象
 * @Author 栗子
 * @Version 1.0
 **/
public class SystemInfo {

    private int core; // 几核

    private int averageMHz; // 平均兆赫

    private int memoryTotal; // 内存大小 （单位GB）

    private int hardDiskSize; // 硬盘大小 （单位GB）

    private boolean hadPublicIp; // 是否有公网IP

    private int bandWidth; // 公网ip带宽（Mbps），如果没有公网ip，这个值就是0

    private int tradePerformance; // 交易处理性能

    private Long[] openServices; // 开启的服务列表

    public int getCore() {
        return core;
    }

    public void setCore(int core) {
        this.core = core;
    }

    public int getAverageMHz() {
        return averageMHz;
    }

    public void setAverageMHz(int averageMHz) {
        this.averageMHz = averageMHz;
    }

    public int getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(int memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public int getHardDiskSize() {
        return hardDiskSize;
    }

    public void setHardDiskSize(int hardDiskSize) {
        this.hardDiskSize = hardDiskSize;
    }

    public boolean isHadPublicIp() {
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

    public int getTradePerformance() {
        return tradePerformance;
    }

    public void setTradePerformance(int tradePerformance) {
        this.tradePerformance = tradePerformance;
    }

    public Long[] getOpenServices() {
        return openServices;
    }

    public void setOpenServices(Long[] openServices) {
        this.openServices = openServices;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
