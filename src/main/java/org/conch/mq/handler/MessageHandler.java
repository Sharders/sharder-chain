package org.conch.mq.handler;

import org.conch.mq.Message;

/**
 * @author CloudSen
 */
public interface MessageHandler {
    /**
     * handle different type of message
     * @param message message object
     * @return whether success
     */
    boolean handleMessage(Message message);
}
