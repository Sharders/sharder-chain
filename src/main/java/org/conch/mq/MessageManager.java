package org.conch.mq;

import org.conch.common.UrlManager;
import org.conch.mq.handler.MessageHandler;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.conch.util.ThreadPool;

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
    private static final Runnable PENDING_MESSAGE_WORKER;
    private static final Runnable SUCCESS_MESSAGE_WORKER;
    private static final Runnable FAILED_MESSAGE_WORKER;

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
        PENDING_MESSAGE_WORKER = new HandleMessageWorker(QueueType.PENDING);
        SUCCESS_MESSAGE_WORKER = new HandleMessageWorker(QueueType.SUCCESS);
        FAILED_MESSAGE_WORKER = new HandleMessageWorker(QueueType.FAILED);
        ThreadPool.scheduleThread("pending message consumer", PENDING_MESSAGE_WORKER, 30);
        ThreadPool.scheduleThread("success message consumer", SUCCESS_MESSAGE_WORKER, 30);
        ThreadPool.scheduleThread("failed message consumer", FAILED_MESSAGE_WORKER, 30);
    }

    public static void init() {
        Logger.logInfoMessage("message queue is initializing...");
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
            Logger.logInfoMessage(Convert.stringTemplate("adding message: {}, to {} queue...", message, queueType));
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
                Logger.logInfoMessage(Convert.stringTemplate("success to add message, now message queue has {} messages", queue.size()));
            } else {
                Logger.logWarningMessage("failed to add message, message queue has been filled...");
            }
        } catch (IllegalStateException | InterruptedException | IllegalArgumentException e) {
            Logger.logErrorMessage(Convert.stringTemplate("[ERROR] failed to add message: {}", message), e);
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
            Logger.logInfoMessage(Convert.stringTemplate("fetching message from {} queue...", queueType));
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
                Logger.logInfoMessage(Convert.stringTemplate("success to fetch message: {}, now {} queue has {} messages", message, queueType, queue.size()));
            } else {
                Logger.logWarningMessage("failed to fetch message, time out...");
            }
        } catch (IllegalArgumentException | NoSuchElementException | InterruptedException e) {
            Logger.logErrorMessage(Convert.stringTemplate("[ERROR] failed to fetch message from {} queue", queueType), e);
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

    static class HandleMessageWorker implements Runnable {

        private MessageHandler messageHandler;
        private QueueType queueType;

        public HandleMessageWorker(QueueType queueType) {
            this.queueType = queueType;
        }

        @Override
        public void run() {
            Message message = fetchMessage(queueType, OperationType.POLL);
            if (message == null) {
                Logger.logInfoMessage(Convert.stringTemplate("{} Thread：no message fetched, nothing to do.", queueType));
                return;
            }
            messageHandler = MessageHandler.Factory.getByType(Message.Type.getTypeByName(message.getType()));
            messageHandler.handleMessage(message, queueType);
        }
    }
}
