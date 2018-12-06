package org.conch.consensus.poc.hardware;

import org.json.simple.JSONObject;

/**
 * @ClassName SystemInfo
 * @Description  系统信息对象
 * @Author 栗子
 * @Version 1.0
 **/
public class SystemInfo {

    private int core; // 几核

    private int averageMHz; // 平均兆赫

    private int memoryTotal; // 内存大小 （单位G）

    private int hardDiskSize; // 硬盘大小 （单位G）

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

    @Override
    public String toString() {
        return "SystemInfo{" +
                "core=" + core +
                ", averageMHz=" + averageMHz +
                ", memoryTotal=" + memoryTotal +
                ", hardDiskSize=" + hardDiskSize +
                '}';
    }
}
