package org.conch.mq;

/**
 * mq message
 *
 * @author CloudSen
 */
public class Constants {
    /**
     * thread name
     */
    static final String PENDING_THREAD_NAME = "pending message consumer";
    static final String SUCCESS_THREAD_NAME = "success message consumer";
    static final String FAILED_THREAD_NAME = "failed message consumer";

    /**
     * initializing message
     */
    static final String MQ_INIT_MSG = "message queue is initializing...";

    /**
     * Message Manager
     */
    static final String ADD_MSG_INFO = "adding message: {}, to {} queue...";
    static final String OPERATION_NOT_FOUND = "operation type can not found...";
    static final String SUCCESS_ADD_MSG = "success to add {} message, now {} queue has {} messages";
    static final String FAILED_ADD_MSG = "failed to add {} message, {} message queue has been filled...";
    static final String ERROR_ADD_MSG = "[ERROR] failed to add {} message: {}";
    static final String FETCH_MSG_INFO = "fetching message from {} queue...";
    static final String SUCCESS_FETCH_MSG = "success to fetch {} message: {}, now {} queue has {} messages";
    static final String FAILED_FETCH_MSG = "failed to fetch message, time out...";
    static final String ERROR_FETCH_MSG = "[ERROR] failed to fetch message from {} queue";
    static final String QUEUE_TYPE_NOT_FOUND = "queue type can not found...";
    static final String NO_MSG_NOW = "{} Thread：no message fetched, nothing to do.";

    /**
     * message handler
     */
    public static final String HANDLE_PENDING_MSG = "handling pending message: {}";
    public static final String SUCCESS_HANDLE_PENDING_MSG = "success to handle pending message:{}";
    public static final String FAILED_HANDLE_PENDING_MSG = "failed to handle pending message:{}";
    public static final String ERROR_HANDLE_PENDING_MSG = "[ERROR] handle pending message error";
    public static final String HANDLE_SUCCESS_MSG = "handling success message: {}";
    public static final String SUCCESS_HANDLE_SUCCESS_MSG = "successfully handle success message:{}";
    public static final String REPROCESS_SUCCESS_MSG_LATER = "the current success message will be reprocessed later...";
    public static final String ERROR_HANDLE_SUCCESS_MSG = "[ERROR] send success message error, failed to connect to operation system";
    public static final String HANDLE_FAILED_MSG = "handling failed message: {}";
    public static final String SUCCESS_HANDLE_FAILED_MSG_1 = "success to handle failed message:{}, return it to pending queue";
    public static final String REPROCESS_FAILED_MSG_LATER = "the current failed message will be reprocessed later...";
    public static final String SUCCESS_HANDLE_FAILED_MSG_2 = "success to handle failed message:{}, return it to operation system";
    public static final String ABANDON_MSG = "the current failed message:{}, will be abandoned...";
    public static final String ERROR_HANDLE_FAILED_MSG = "[ERROR] send failed message error, failed to connect to operate system";

    /**
     * node config performance handler
     */
    public static final String NODE_CONFIG_SUCCESS_INFO = "node configuration performance success message:{} has been sent to Operation System";
    public static final String NODE_CONFIG_FAILED_INFO = "node configuration performance success message failed to send to Operation System";
    public static final String NODE_CONFIG_SUCCESS_INFO_2 = "node configuration performance failed message has been sent to Operation System";
    public static final String NODE_CONFIG_FAILED_INFO_2 = "node configuration performance failed message failed to send to Operation System";
}
