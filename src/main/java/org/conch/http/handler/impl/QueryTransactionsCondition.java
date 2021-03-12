package org.conch.http.handler.impl;

import org.conch.http.PocTxApi;
import org.conch.http.handler.QueryTransactionsHandler;

/**
 * @author CloudSen
 */
public class QueryTransactionsCondition {

    private QueryTransactionsHandler.HandleType handleType;
    private byte type;
    private byte subType;
    private String ip;
    private String port;
    private Long templateVersion;

    public QueryTransactionsHandler.HandleType getHandleType() {
        return handleType;
    }

    public QueryTransactionsCondition setHandleType(QueryTransactionsHandler.HandleType handleType) {
        this.handleType = handleType;
        return this;
    }

    public byte getType() {
        return type;
    }

    public QueryTransactionsCondition setType(byte type) {
        this.type = type;
        return this;
    }

    public byte getSubType() {
        return subType;
    }

    public QueryTransactionsCondition setSubType(byte subType) {
        this.subType = subType;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public QueryTransactionsCondition setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public String getPort() {
        return port;
    }

    public QueryTransactionsCondition setPort(String port) {
        this.port = port;
        return this;
    }

    public Long getTemplateVersion() {
        return templateVersion;
    }

    public QueryTransactionsCondition setTemplateVersion(Long templateVersion) {
        this.templateVersion = templateVersion;
        return this;
    }
}
