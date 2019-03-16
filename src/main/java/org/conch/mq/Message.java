package org.conch.mq;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * message for message queue
 *
 * @author CloudSen
 */
public class Message implements Serializable {

    private static final long serialVersionUID = -3563275674424414022L;

    public enum Type {
        /**
         * node config performance test
         */
        NODE_CONFIG_PERFORMANCE_TEST("nodeConfigPerformanceTest"),
        ;

        private String name;

        public String getName() {
            return name;
        }

        Type(String name) {
            this.name = name;
        }

        public static Type getTypeByName(String name) {
            return Arrays.stream(Type.values())
                    .filter(type -> type.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        }
    }

    /**
     * message id
     */
    private String id;

    /**
     * message sender
     */
    private String sender;

    /**
     * message type
     */
    private String type;

    /**
     * when the message has been sent
     */
    private long timestamp;

    /**
     * extra data
     */
    private String dataJson;

    /**
     * retry count
     */
    private Integer retryCount;

    /**
     *
     */
    private Boolean success;

    public String getId() {
        return id;
    }

    public Message setId(String id) {
        this.id = id;
        return this;
    }

    public String getSender() {
        return sender;
    }

    public Message setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getType() {
        return type;
    }

    public Message setType(String type) {
        this.type = type;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Message setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getDataJson() {
        return dataJson;
    }

    public Message setDataJson(String dataJson) {
        this.dataJson = dataJson;
        return this;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Message setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public Boolean getSuccess() {
        return success;
    }

    public Message setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    public Message() {
        this.retryCount = 0;
        this.success = false;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return getTimestamp() == message.getTimestamp() &&
                Objects.equals(getId(), message.getId()) &&
                getSender().equals(message.getSender()) &&
                getType().equals(message.getType()) &&
                Objects.equals(getDataJson(), message.getDataJson()) &&
                Objects.equals(getRetryCount(), message.getRetryCount()) &&
                Objects.equals(getSuccess(), message.getSuccess());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSender(), getType(), getTimestamp(), getDataJson(), getRetryCount(), getSuccess());
    }
}
