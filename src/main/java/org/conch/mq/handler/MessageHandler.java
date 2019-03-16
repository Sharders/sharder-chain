package org.conch.mq.handler;

import org.conch.mq.Message;
import org.conch.mq.MessageManager;

import java.util.Arrays;

/**
 * handler for message
 *
 * @author CloudSen
 */
public interface MessageHandler {

    enum Factory {
        /**
         * node configuration performance test handler
         */
        NODE_CONFIG_PERFORMANCE_TEST(Message.Type.NODE_CONFIG_PERFORMANCE_TEST, NodeConfigPerformanceMsgHandler.getInstance()),
        ;

        private Message.Type type;
        private MessageHandler messageHandler;

        Factory(Message.Type type, MessageHandler messageHandler) {
            this.type = type;
            this.messageHandler = messageHandler;
        }

        public Message.Type getType() {
            return type;
        }

        public MessageHandler getMessageHandler() {
            return messageHandler;
        }

        public static MessageHandler getByType(Message.Type type) {
            return Arrays.stream(Factory.values())
                    .filter(handler -> handler.getType().equals(type))
                    .map(Factory::getMessageHandler).findFirst().orElse(null);
        }

        public static String getTypeNameByHandler(MessageHandler messageHandler) {
            return Arrays.stream(Factory.values())
                    .filter(handler -> handler.getMessageHandler() == messageHandler)
                    .map(Factory::getType).map(Message.Type::getName).findFirst().orElse(null);
        }
    }

    /**
     * handle different type of message
     *
     * @param message   message object
     * @param queueType queue type
     * @return whether success
     */
    default boolean handleMessage(Message message, MessageManager.QueueType queueType) {
        return false;
    }
}
