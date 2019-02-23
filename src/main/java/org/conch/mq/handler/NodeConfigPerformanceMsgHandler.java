package org.conch.mq.handler;

import com.alibaba.fastjson.JSON;
import org.conch.Conch;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.mq.Message;
import org.conch.mq.MessageManager;
import org.conch.mq.dto.NodeConfigPerformanceTestDto;
import org.conch.util.Convert;
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
            case RECEIVED:
                this.handleReceived(message);
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
    private boolean handleReceived(Message message) {
        boolean result = false;
        System.out.println(Convert.stringTemplate("handling received message: {}", message));
        try {
            NodeConfigPerformanceTestDto data = JSON.parseObject(message.getDataJson(), NodeConfigPerformanceTestDto.class);
            GetNodeHardware.readAndReport(data.getTestTime());
            result = MessageManager.addSuccessMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            MessageManager.addFailedMessage(message);
        }
        return result;
    }

    private boolean handleSuccess(Message message) {
        boolean result = false;
        String address = Conch.getMyAddress();
        Message msg = new Message().setId(message.getId()).setSender(address)
                .setDataJson(message.getDataJson()).setTimestamp(message.getTimestamp()).setType(message.getType());
        System.out.println(Convert.stringTemplate("handling success message: {}", message));
        try {
            RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(
                    UrlManager.getFoundationUrl(
                            UrlManager.ADD_MESSAGE_EOLINKER,
                            UrlManager.ADD_MESSAGE_PATH
                    )
            )
                    .post()
                    .body(msg)
                    .request();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[ERROR] send message error, failed to connect to operate system");
            System.out.println("the current message will be resend later...");
            MessageManager.receiveMessage(message);
        }
        return result;
    }

    private boolean handleFailed(Message message) {
        boolean result = false;
        System.out.println(Convert.stringTemplate("handling failed message: {}", message));

        return result;
    }
}
