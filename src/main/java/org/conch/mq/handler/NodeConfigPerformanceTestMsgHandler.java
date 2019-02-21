package org.conch.mq.handler;

import com.alibaba.fastjson.JSON;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.mq.Message;
import org.conch.mq.dto.NodeConfigPerformanceTestDto;
import org.conch.util.Convert;

/**
 * @author CloudSen
 */
public class NodeConfigPerformanceTestMsgHandler implements MessageHandler {

    private NodeConfigPerformanceTestMsgHandler() {
    }

    private static class InstanceHolder {
        private static final NodeConfigPerformanceTestMsgHandler INSTANCE = new NodeConfigPerformanceTestMsgHandler();
    }

    public static NodeConfigPerformanceTestMsgHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public boolean handleMessage(Message message) {
        boolean result = false;
        SystemInfo systemInfo = new SystemInfo();
        System.out.println(Convert.stringTemplate("handling message: {}", message));
        try {
            NodeConfigPerformanceTestDto data = JSON.parseObject(message.getDataJson(), NodeConfigPerformanceTestDto.class);
            GetNodeHardware.read(systemInfo, data.getTestTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
