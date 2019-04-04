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
    private static boolean openLogger = false;

    private static void log(String msg,Logger.Level level){
        if(!openLogger) return;

        if(Logger.Level.DEBUG == level) {
            Logger.logDebugMessage(msg);
        }else if(Logger.Level.INFO == level){
            Logger.logInfoMessage(msg);
        }else if(Logger.Level.WARN == level){
            Logger.logWarningMessage(msg);
        }
    }

    private static void log(String msg, Throwable e) {
        if (!openLogger) {
            return;
        }

        Logger.logErrorMessage(msg, e);
    }

    private static final BlockingQueue<Message> PENDING_MESSAGE_QUEUE = new LinkedBlockingQueue<>(500);
    private static final BlockingQueue<Message> SUCCESS_MESSAGE_QUEUE = new LinkedBlockingQueue<>(500);
    private static final BlockingQueue<Message> FAILED_MESSAGE_QUEUE = new LinkedBlockingQueue<>(500);
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
        ThreadPool.scheduleThread(Constants.PENDING_THREAD_NAME, PENDING_MESSAGE_WORKER, 1);
        ThreadPool.scheduleThread(Constants.SUCCESS_THREAD_NAME, SUCCESS_MESSAGE_WORKER, 1);
        ThreadPool.scheduleThread(Constants.FAILED_THREAD_NAME, FAILED_MESSAGE_WORKER, 1);
    }

    public static void init() {
        Logger.logInfoMessage(Constants.MQ_INIT_MSG);
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
            log(Convert.stringTemplate(Constants.ADD_MSG_INFO, message, queueType),Logger.Level.DEBUG);
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
                    throw new IllegalArgumentException(Constants.OPERATION_NOT_FOUND);
            }

            if (result) {
                log(Convert.stringTemplate(Constants.SUCCESS_ADD_MSG, queueType, queueType, queue.size()),Logger.Level.DEBUG);
            } else {
                log(Convert.stringTemplate(Constants.FAILED_ADD_MSG, queueType, queueType),Logger.Level.WARN);
            }
        } catch (IllegalStateException | InterruptedException | IllegalArgumentException e) {
            log(Convert.stringTemplate(Constants.ERROR_ADD_MSG, queueType, message), e);
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
            log(Convert.stringTemplate(Constants.FETCH_MSG_INFO, queueType),Logger.Level.DEBUG);
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
                    throw new IllegalArgumentException(Constants.OPERATION_NOT_FOUND);
            }
            if (message != null) {
                log(Convert.stringTemplate(Constants.SUCCESS_FETCH_MSG, queueType, message, queueType, queue.size()),Logger.Level.DEBUG);
            } else {
                log(Constants.FAILED_FETCH_MSG,Logger.Level.WARN);
            }
        } catch (IllegalArgumentException | NoSuchElementException | InterruptedException e) {
            log(Convert.stringTemplate(Constants.ERROR_FETCH_MSG, queueType), e);
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
                throw new IllegalArgumentException(Constants.QUEUE_TYPE_NOT_FOUND);
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
        String url = UrlManager.getFoundationUrl(
                UrlManager.ADD_MESSAGE_TO_SHARDER_EOLINKER,
                UrlManager.ADD_MESSAGE_TO_SHARDER_LOCAL,
                UrlManager.ADD_MESSAGE_TO_SHARDER_PATH
        );
        return RestfulHttpClient.getClient(url)
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
                log(Convert.stringTemplate(Constants.NO_MSG_NOW, queueType),Logger.Level.DEBUG);
                return;
            }
            messageHandler = MessageHandler.Factory.getByType(Message.Type.getTypeByName(message.getType()));
            messageHandler.handleMessage(message, queueType);
        }
    }
}
