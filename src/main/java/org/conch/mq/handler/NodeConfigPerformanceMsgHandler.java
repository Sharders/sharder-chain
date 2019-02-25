package org.conch.mq.handler;

import com.alibaba.fastjson.JSON;
import org.conch.Conch;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.http.Result;
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
        Logger.logInfoMessage(Convert.stringTemplate("handling pending message: {}", message));
        try {
            NodeConfigPerformanceTestDto data = JSON.parseObject(message.getDataJson(), NodeConfigPerformanceTestDto.class);
            result = GetNodeHardware.readAndReport(data.getTestTime());
            if (result) {
                result = MessageManager.receiveMessage(message, MessageManager.QueueType.SUCCESS, MessageManager.OperationType.PUT);
            } else {
                result = MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
        }
        return result;
    }

    private boolean handleSuccess(Message message) {
        boolean result = false;
        Message msg = new Message().setId(message.getId()).setSender(Conch.getMyAddress()).setRetryCount(0).setSuccess(true)
                .setDataJson(message.getDataJson()).setTimestamp(message.getTimestamp()).setType(message.getType());
        Logger.logInfoMessage(Convert.stringTemplate("handling success message: {}", message));
        try {
            RestfulHttpClient.HttpResponse response = MessageManager.sendMessageToFoundation(msg);
            Result responseResult = JSON.parseObject(response.getContent(), Result.class);
            result = responseResult.getSuccess();
            if (result) {
                Logger.logInfoMessage("node configuration performance success message has been sent to Operation System");
            } else {
                Logger.logWarningMessage("node configuration performance success message failed to send to Operation System");
                Logger.logWarningMessage("the current success message will be reprocessed later...");
                MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
            }
        } catch (IOException e) {
            Logger.logErrorMessage("[ERROR] send success message error, failed to connect to operate system", e);
            Logger.logWarningMessage("the current success message will be reprocessed later...");
            MessageManager.receiveMessage(message, MessageManager.QueueType.FAILED, MessageManager.OperationType.PUT);
        }
        return result;
    }

    private boolean handleFailed(Message message) {
        boolean isValid, result = false;
        Logger.logInfoMessage(Convert.stringTemplate("handling failed message: {}", message));
        MessageManager.addRetryCount(message);
        isValid = MessageManager.checkMsgValidity(message);
        if (isValid) {
            // return it to pending queue
            result = MessageManager.receiveMessage(message, MessageManager.QueueType.PENDING, MessageManager.OperationType.PUT);
        } else {
            // send failed message
            Message msg = new Message().setId(message.getId()).setSender(Conch.getMyAddress()).setRetryCount(0).setSuccess(true)
                    .setDataJson(message.getDataJson()).setTimestamp(message.getTimestamp()).setType(message.getType());
            Logger.logInfoMessage(Convert.stringTemplate("handling failed message: {}", message));
            try {
                RestfulHttpClient.HttpResponse response = MessageManager.sendMessageToFoundation(msg);
                Result responseResult = JSON.parseObject(response.getContent(), Result.class);
                result = responseResult.getSuccess();
                if (result) {
                    Logger.logInfoMessage("node configuration performance failed message has been sent to Operation System");
                } else {
                    Logger.logWarningMessage("node configuration performance failed message failed to send to Operation System");
                    Logger.logWarningMessage("the current failed message will be abandoned...");
                }
            } catch (IOException e) {
                // abandon message
                Logger.logErrorMessage("[ERROR] send failed message error, failed to connect to operate system", e);
                Logger.logWarningMessage("the current failed message will be abandoned...");
            }
        }
        return result;
    }
}
