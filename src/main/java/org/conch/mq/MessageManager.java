package org.conch.mq;

import org.conch.common.UrlManager;
import org.conch.mq.handler.MessageHandler;
import org.conch.util.Convert;
import org.conch.util.RestfulHttpClient;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author CloudSen
 */
public class MessageManager {

    private static final BlockingQueue<Message> PENDING_MESSAGE_QUEUE = new LinkedBlockingQueue<>(50);
    private static final BlockingQueue<Message> SUCCESS_MESSAGE_QUEUE = new LinkedBlockingQueue<>(50);
    private static final BlockingQueue<Message> FAILED_MESSAGE_QUEUE = new LinkedBlockingQueue<>(50);

    public enum QueueType {
        /**
         * PENDING queue
         */
        PENDING,
        /**
         * successfully processed queue
         */
        SUCCESS,
        /**
         * failed to process queue
         */
        FAILED,
    }

    public enum OperationType {
        /**
         * add
         */
        ADD,
        OFFER,
        PUT,
        REMOVE,
        POLL,
        TAKE,
    }

    static {
        /*
         using schedule thread to run consumers
         */
    }

    /**
     * add a new message to pending queue
     *
     * @param message       message
     * @param queueType     pending,success or failed
     * @param operationType add,offer or put
     * @return if success return true, else return false
     */
    public static boolean receiveMessage(Message message, QueueType queueType, OperationType operationType) {
        boolean result = false;
        BlockingQueue<Message> queue;
        try {
            queue = getQueueByType(queueType);
            System.out.println(Convert.stringTemplate("adding message: {}, to {} queue...", message, queueType));
            switch (operationType) {
                case ADD:
                    result = queue.add(message);
                    break;
                case OFFER:
                    result = queue.offer(message, 5, TimeUnit.SECONDS);
                    break;
                case PUT:
                    queue.put(message);
                    result = true;
                    break;
                default:
                    throw new IllegalArgumentException("operation type can not found...");
            }

            if (result) {
                System.out.println(Convert.stringTemplate("success to add message, now message queue has {} messages", queue.size()));
            } else {
                System.out.println("failed to add message, message queue has been filled...");
            }
        } catch (IllegalStateException | InterruptedException | IllegalArgumentException e) {
            System.out.println(Convert.stringTemplate("[ERROR] failed to add message: {}", message));
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Return message at the head of message queue
     *
     * @param queueType     pending,success or failed
     * @param operationType remove,poll or take
     * @return message or null
     */
    public static Message fetchMessage(QueueType queueType, OperationType operationType) {
        Message message = null;
        BlockingQueue<Message> queue;

        try {
            queue = getQueueByType(queueType);
            System.out.println(Convert.stringTemplate("fetching message from {} queue...", queueType));
            switch (operationType) {
                case REMOVE:
                    message = queue.remove();
                    break;
                case POLL:
                    message = queue.poll(5, TimeUnit.SECONDS);
                    break;
                case TAKE:
                    message = queue.take();
                    break;
                default:
                    throw new IllegalArgumentException("operation type can not found...");
            }
            if (message != null) {
                System.out.println(Convert.stringTemplate("success to fetch message: {}, now {} queue has {} messages", message, queueType, queue.size()));
            } else {
                System.out.println("failed to fetch message, time out...");
            }
        } catch (IllegalArgumentException | NoSuchElementException | InterruptedException e) {
            System.out.println(Convert.stringTemplate("[ERROR] failed to fetch message from {} queue", queueType));
            e.printStackTrace();
        }
        return message;
    }

    private static BlockingQueue<Message> getQueueByType(QueueType queueType) {
        switch (queueType) {
            case PENDING:
                return PENDING_MESSAGE_QUEUE;
            case SUCCESS:
                return SUCCESS_MESSAGE_QUEUE;
            case FAILED:
                return FAILED_MESSAGE_QUEUE;
            default:
                throw new IllegalArgumentException("queue type can not found...");
        }
    }

    /**
     * If a message has been processed failed  more than 3 times, then abandon it
     *
     * @param message message
     * @return true: is valid; false: need abandon
     */
    public static boolean checkMsgValidity(Message message) {
        return message.getRetryCount() <= 3;
    }

    public static void addRetryCount(Message message) {
        message.setRetryCount(message.getRetryCount() + 1);
    }

    public static void resetRetryCount(Message message) {
        message.setRetryCount(0);
    }

    public static RestfulHttpClient.HttpResponse sendMessageToFoundation(Message message) throws IOException {
        return RestfulHttpClient.getClient(
                UrlManager.getFoundationUrl(UrlManager.ADD_MESSAGE_TO_SHARDER_EOLINKER, UrlManager.ADD_MESSAGE_TO_SHARDER_PATH)
        )
                .post()
                .body(message)
                .request();
    }

    class HandleMessageWorker implements Runnable {

        private MessageHandler messageHandler;
        private QueueType queueType;

        public HandleMessageWorker(QueueType queueType) {
            this.queueType = queueType;
        }

        @Override
        public void run() {
            Message message = fetchMessage(queueType, OperationType.POLL);
            if (message == null) {
                System.out.println(Convert.stringTemplate("{} Thread：no message fetched, nothing to do.", queueType));
                return;
            }
            messageHandler = MessageHandler.Factory.getByType(Message.Type.getTypeByName(message.getType()));
            messageHandler.handleMessage(message, queueType);
        }
    }
}
