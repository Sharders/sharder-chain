package org.conch.mq.handler;

import org.conch.mq.Message;
import org.conch.mq.MessageManager;

/**
 * handler for message
 * @author CloudSen
 */
public interface MessageHandler {
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
