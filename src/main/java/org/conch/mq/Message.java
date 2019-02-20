package org.conch.mq;

/**
 * 消息队列中的消息
 *
 * @author CloudSen
 */
public class Message<T> {

    enum Type {
        /**
         * 节点配置性能测试
         */
        NODE_CONFIG_PERFORMANCE_TEST("nodeConfigPerformanceTest"),
        ;

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 消息ID，可以防止重复消息
     */
    private String id;

    /**
     * 消息发送者
     */
    private String sender;

    /**
     * 消息类型
     */
    private Type type;

    /**
     * 传输的数据
     */
    private T data;


    public String getId() {
        return id;
    }

    public Message<T> setId(String id) {
        this.id = id;
        return this;
    }

    public String getSender() {
        return sender;
    }

    public Message<T> setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Message<T> setType(Type type) {
        this.type = type;
        return this;
    }

    public T getData() {
        return data;
    }

    public Message<T> setData(T data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", type=" + type.getName() +
                ", data=" + data +
                '}';
    }
}
