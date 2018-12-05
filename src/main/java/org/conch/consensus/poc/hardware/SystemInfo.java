package org.conch.consensus.poc.hardware;

/**
 * @ClassName SystemInfo
 * @Description  系统信息对象
 * @Author 栗子
 * @Version 1.0
 **/
public class SystemInfo {

    private int core;

    private int averageMHz;

    private int memoryTotal;

    private int HardDiskSize;

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
        return HardDiskSize;
    }

    public void setHardDiskSize(int hardDiskSize) {
        HardDiskSize = hardDiskSize;
    }

    @Override
    public String toString() {
        return "SystemInfo{" +
                "core=" + core +
                ", averageMHz=" + averageMHz +
                ", memoryTotal=" + memoryTotal +
                ", HardDiskSize=" + HardDiskSize +
                '}';
    }
}
