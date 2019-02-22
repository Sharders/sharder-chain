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

    private static final BlockingQueue<Message> RECEIVED_MESSAGE_QUEUE = new LinkedBlockingQueue<>(50);
    private static final BlockingQueue<Message> SUCCESS_MESSAGE_QUEUE = new LinkedBlockingQueue<>(50);
    private static final BlockingQueue<Message> FAILED_MESSAGE_QUEUE = new LinkedBlockingQueue<>(50);
    private static final AtomicInteger COUNT = new AtomicInteger(0);

    public enum QueueType {
        /**
         * 接收消息队列
         */
        RECEIVED,
        /**
         * 处理成功队列
         */
        SUCCESS,
        /**
         * 处理失败队列
         */
        FAILED;

        QueueType() {
        }
    }

    static {
        /*
         using schedule thread to run consumers
         */
    }

    /**
     * add a new message to queue with time out
     *
     * @param message message
     * @return if success return true, else return false
     */
    public static boolean receiveMessage(Message message) {
        boolean result = false;
        try {
            System.out.println(Convert.stringTemplate("adding message: {}", message));
            result = RECEIVED_MESSAGE_QUEUE.offer(message, 5, TimeUnit.SECONDS);
            if (result) {
                System.out.println(Convert.stringTemplate("success to add message, now message queue has {} messages", RECEIVED_MESSAGE_QUEUE.size()));
            } else {
                System.out.println("failed to add message, message queue has been filled...");
            }
        } catch (InterruptedException e) {
            System.out.println(Convert.stringTemplate("[ERROR] failed to add message: {}", message));
            e.printStackTrace();
        }
        return result;
    }

    /**
     * add a new message to queue, if queue is full, won't return false, just wait
     * @param message new message
     * @return whether success
     */
    public static boolean receiveMessageUntilIdel(Message message) {
        boolean result = false;
        try {
            System.out.println(Convert.stringTemplate("adding message: {}", message));
            RECEIVED_MESSAGE_QUEUE.put(message);
            System.out.println(Convert.stringTemplate("success to add message, now message queue has {} messages", RECEIVED_MESSAGE_QUEUE.size()));
            result = true;
        } catch (InterruptedException e) {
            System.out.println(Convert.stringTemplate("[ERROR] failed to add message: {}", message));
            e.printStackTrace();
        }
        return result;
    }

    public static boolean addSuccessMessage(Message message) {
        boolean result = false;
        try {
            System.out.println(Convert.stringTemplate("adding success message: {}", message));
            result = SUCCESS_MESSAGE_QUEUE.offer(message, 5, TimeUnit.SECONDS);
            if (result) {
                System.out.println(Convert.stringTemplate("success to add message, now success message queue has {} messages", RECEIVED_MESSAGE_QUEUE.size()));
            } else {
                System.out.println("failed to add message, success message queue has been filled...");
            }
        } catch (InterruptedException e) {
            System.out.println(Convert.stringTemplate("[ERROR] failed to add success message: {}", message));
            e.printStackTrace();
        }
        return result;
    }

    public static boolean addFailedMessage(Message message) {
        boolean result = false;
        try {
            System.out.println(Convert.stringTemplate("adding failed message: {}", message));
            result = FAILED_MESSAGE_QUEUE.offer(message, 5, TimeUnit.SECONDS);
            if (result) {
                System.out.println(Convert.stringTemplate("success to add message, now failed message queue has {} messages", RECEIVED_MESSAGE_QUEUE.size()));
            } else {
                System.out.println("failed to add message, failed message queue has been filled...");
            }
        } catch (InterruptedException e) {
            System.out.println(Convert.stringTemplate("[ERROR] failed to add failed message: {}", message));
            e.printStackTrace();
        }
        return result;
    }

    class HandleMessageWorker implements Runnable {

        private MessageHandler messageHandler;
        private QueueType queueType;

        public HandleMessageWorker(QueueType queueType) {
            this.queueType = queueType;
        }

        @Override
        public void run() {
            try {
                Message message = RECEIVED_MESSAGE_QUEUE.poll(5, TimeUnit.SECONDS);
                if (message == null) {
                    System.out.println("no message received, nothing to do.");
                    return;
                }
                messageHandler = Message.Handler.getByType(message.getType());
                messageHandler.handleMessage(message, queueType);

            } catch (InterruptedException e) {
                System.out.println("[ERROR] message handler has been interrupted!");
                e.printStackTrace();
            }
        }
    }
}
