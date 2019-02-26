package org.conch.mq.dto;

import java.io.Serializable;

/**
 * @author CloudSen
 */
public class NodeConfigPerformanceTestDto implements Serializable {

    private static final long serialVersionUID = -1780967390022874424L;
    private Integer id;
    private String address;
    private Integer port;
    private String netWorkType;
    private Integer testTime;

    public Integer getId() {
        return id;
    }

    public NodeConfigPerformanceTestDto setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public NodeConfigPerformanceTestDto setAddress(String address) {
        this.address = address;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public NodeConfigPerformanceTestDto setPort(Integer port) {
        this.port = port;
        return this;
    }

    public Integer getTestTime() {
        return testTime;
    }

    public String getNetWorkType() {
        return netWorkType;
    }

    public NodeConfigPerformanceTestDto setNetWorkType(String netWorkType) {
        this.netWorkType = netWorkType;
        return this;
    }

    public NodeConfigPerformanceTestDto setTestTime(Integer testTime) {
        this.testTime = testTime;
        return this;
    }
}
