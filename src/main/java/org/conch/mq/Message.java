package org.conch.mq;

import org.apache.commons.lang3.StringUtils;
import org.conch.mq.handler.MessageHandler;
import org.conch.mq.handler.NodeConfigPerformanceTestMsgHandler;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * 消息队列中的消息
 *
 * @author CloudSen
 */
public class Message implements Serializable {

    private static final long serialVersionUID = -3563275674424414022L;

    public enum Handler {
        /**
         * 节点配置性能测试
         */
        NODE_CONFIG_PERFORMANCE_TEST("nodeConfigPerformanceTest", NodeConfigPerformanceTestMsgHandler.getInstance()),
        ;

        private String type;
        private MessageHandler messageHandler;

        Handler(String type, MessageHandler messageHandler) {
            this.type = type;
            this.messageHandler = messageHandler;
        }

        public String getType() {
            return type;
        }

        public MessageHandler getMessageHandler() {
            return messageHandler;
        }

        public static boolean containsType(String type) {
            if (StringUtils.isEmpty(type)) {
                return false;
            }
            return Arrays.stream(Handler.values())
                    .anyMatch(handler -> handler.getType().equalsIgnoreCase(type));
        }

        public static MessageHandler getByType(String type) {
            return Arrays.stream(Handler.values())
                    .filter(handler -> handler.getType().equalsIgnoreCase(type))
                    .map(Handler::getMessageHandler).findFirst().orElse(null);
        }

        public static String getTypeNameByHandler(MessageHandler messageHandler) {
            return Arrays.stream(Handler.values())
                    .filter(handler -> handler.getMessageHandler() == messageHandler)
                    .map(Handler::getType).findFirst().orElse(null);
        }
    }

    /**
     * 消息ID
     */
    private String id;

    /**
     * 消息发送者
     */
    private String sender;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 标记是否是同一时刻发出的
     */
    private long timestamp;

    /**
     * 传输的数据
     */
    private String dataJson;

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

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", dataJson='" + dataJson + '\'' +
                '}';
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
        return timestamp == message.timestamp &&
                id.equals(message.id) &&
                sender.equals(message.sender) &&
                type.equals(message.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sender, type, timestamp);
    }
}
