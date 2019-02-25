package org.conch.mq.handler;

import com.alibaba.fastjson.JSON;
import org.conch.Conch;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.http.Result;
import org.conch.mq.Constants;
import org.conch.mq.Message;
import org.conch.mq.MessageManager;
import org.conch.mq.dto.NodeConfigPerformanceTestDto;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

import java.io.IOException;

/**
 * handle coming node configuration performance
 *
 * @author CloudSen
 */
public class NodeConfigPerformanceMsgHandler implements MessageHandler {

    private NodeConfigPerformanceMsgHandler() {
    }

    private static class InstanceHolder {
        private static final NodeConfigPerformanceMsgHandler INSTANCE = new NodeConfigPerformanceMsgHandler();
    }

    public static NodeConfigPerformanceMsgHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public boolean handleMessage(Message message, MessageManager.QueueType queueType) {
        switch (queueType) {
            case PENDING:
                this.handlePending(message);
                break;
            case SUCCESS:
                this.handleSuccess(message);
                break;
            case FAILED:
                this.handleFailed(message);
                break;
            default:
                break;
        }
        return false;
    }

    /**
     * do performance test, and then report it to operation system.<br>
     * if success add to success queue, else add to failed queue.
     *
     * @param message message
     * @return whether success
     */
    private boolean handlePending(Message message) {
        boolean result = false;
        Logger.logInfoMessage(Convert.stringTemplate(Constants.HANDLE_PENDING_MSG, message));
        try {
            NodeConfigPerformanceTestDto data = JSON.parseObject(message.getDataJson(), NodeConfigPerformanceTestDto.class);
            result = GetNodeHardware.readAndReport(data.getTestTime());
            if (result) {
                message.setSuccess(true);
                Logger.logInfoMessage(Convert.stringTemplate(Constants.SUCCESS_HANDLE_PENDING_MSG, message));
                result = MessageManager.receiveMessage(message, MessageManager.QueueType.SUCCESS, MessageManager.OperationType.PUT);
            } else {
                message.setSuccess(false);
                Logger.logInfoMessage(Convert.stringTemplate(Constants.FAILED_HANDLE_PENDING_MSG, message));
                result = MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
            }
        } catch (Exception e) {
            Logger.logErrorMessage(Constants.ERROR_HANDLE_PENDING_MSG, e);
            MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
        }
        return result;
    }

    private boolean handleSuccess(Message message) {
        boolean result = false;
        Message msg = new Message().setId(message.getId()).setSender(Conch.getMyAddress()).setRetryCount(0).setSuccess(true)
                .setDataJson(message.getDataJson()).setTimestamp(message.getTimestamp()).setType(message.getType());
        Logger.logInfoMessage(Convert.stringTemplate(Constants.HANDLE_SUCCESS_MSG, message));
        try {
            RestfulHttpClient.HttpResponse response = MessageManager.sendMessageToFoundation(msg);
            Result responseResult = JSON.parseObject(response.getContent(), Result.class);
            result = responseResult.getSuccess();
            if (result) {
                Logger.logInfoMessage(Convert.stringTemplate(Constants.SUCCESS_HANDLE_SUCCESS_MSG, message));
                Logger.logInfoMessage(Convert.stringTemplate(Constants.NODE_CONFIG_SUCCESS_INFO, msg));
            } else {
                message.setSuccess(false);
                Logger.logWarningMessage(Constants.NODE_CONFIG_FAILED_INFO);
                Logger.logWarningMessage(Constants.REPROCESS_SUCCESS_MSG_LATER);
                MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
            }
        } catch (IOException e) {
            Logger.logErrorMessage(Constants.ERROR_HANDLE_SUCCESS_MSG, e);
            Logger.logWarningMessage(Constants.REPROCESS_SUCCESS_MSG_LATER);
            MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
        }
        return result;
    }

    private boolean handleFailed(Message message) {
        boolean isValid, result = false;
        Logger.logInfoMessage(Convert.stringTemplate(Constants.HANDLE_FAILED_MSG, message));
        MessageManager.addRetryCount(message);
        isValid = MessageManager.checkMsgValidity(message);
        if (isValid) {
            // return it to pending queue
            Logger.logInfoMessage(Convert.stringTemplate(Constants.SUCCESS_HANDLE_FAILED_MSG_1, message));
            Logger.logInfoMessage(Constants.REPROCESS_FAILED_MSG_LATER);
            result = MessageManager.receiveMessage(message, MessageManager.QueueType.PENDING, MessageManager.OperationType.PUT);
        } else {
            // send failed message
            Message msg = new Message().setId(message.getId()).setSender(Conch.getMyAddress()).setRetryCount(0).setSuccess(true)
                    .setDataJson(message.getDataJson()).setTimestamp(message.getTimestamp()).setType(message.getType());
            try {
                RestfulHttpClient.HttpResponse response = MessageManager.sendMessageToFoundation(msg);
                Result responseResult = JSON.parseObject(response.getContent(), Result.class);
                result = responseResult.getSuccess();
                if (result) {
                    Logger.logInfoMessage(Convert.stringTemplate(Constants.SUCCESS_HANDLE_FAILED_MSG_2, message));
                    Logger.logInfoMessage(Constants.NODE_CONFIG_SUCCESS_INFO_2);
                } else {
                    Logger.logWarningMessage(Constants.NODE_CONFIG_FAILED_INFO_2);
                    Logger.logWarningMessage(Convert.stringTemplate(Constants.ABANDON_MSG, message));
                }
            } catch (IOException e) {
                // abandon message
                Logger.logErrorMessage(Constants.ERROR_HANDLE_FAILED_MSG, e);
                Logger.logWarningMessage(Convert.stringTemplate(Constants.ABANDON_MSG, message));
            }
        }
        return result;
    }
}
