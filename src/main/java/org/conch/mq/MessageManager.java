package org.conch.mq;

import org.conch.mq.handler.MessageHandler;
import org.conch.util.Convert;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author CloudSen
 */
public class MessageManager {

    private static final BlockingQueue<Message> MESSAGE_QUEEN = new LinkedBlockingQueue<>(50);
    private static final BlockingQueue<Message> SUCCESS_MESSAGE_QUEEN = new LinkedBlockingQueue<>(50);
    private static final AtomicInteger COUNT = new AtomicInteger(0);

    /**
     * add message to queue
     *
     * @param message message
     * @return if success return true, else return false
     */
    public static boolean addMessage(Message message) {
        boolean result = false;
        try {
            System.out.println(Convert.stringTemplate("adding message: {}", message));
            result = MESSAGE_QUEEN.offer(message, 5, TimeUnit.SECONDS);
            if (result) {
                System.out.println(Convert.stringTemplate("success to add message, now message queue has {} messages", MESSAGE_QUEEN.size()));
            } else {
                System.out.println("failed to add message, message queue has been filled...");
            }
        } catch (InterruptedException e) {
            System.out.println(Convert.stringTemplate("[ERROR] failed to add message: {}", message));
            e.printStackTrace();
        }
        return result;
    }

    class HandleMessageWorker implements Runnable {

        private MessageHandler messageHandler;

        @Override
        public void run() {
            try {
                Message message = MESSAGE_QUEEN.poll(5, TimeUnit.SECONDS);
                if (message == null) {
                    System.out.println("no message received, nothing to do.");
                    return;
                }
                messageHandler = Message.Handler.getByType(message.getType());
                messageHandler.handleMessage(message);

            } catch (InterruptedException e) {
                System.out.println("[ERROR] message handler has been interrupted!");
                e.printStackTrace();
            }
        }
    }
}
