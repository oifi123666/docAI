package com.javaee.aiservice.agent.execution.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 实时进度广播器。若 WebSocket 消息模板不可用，则静默降级，不影响主流程。
 */
@Component
public class AgentProgressBroadcaster {

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void publish(AgentProgressEvent event) {
        if (messagingTemplate == null || event == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSend("/topic/agent/progress", event);
            if (event.getUserId() != null && !event.getUserId().isBlank()) {
                messagingTemplate.convertAndSend("/topic/agent/users/" + event.getUserId(), event);
            }
            if (event.getTraceId() != null && !event.getTraceId().isBlank()) {
                messagingTemplate.convertAndSend("/topic/agent/tasks/" + event.getTraceId(), event);
            }
            if (event.getJobId() != null && !event.getJobId().isBlank()) {
                messagingTemplate.convertAndSend("/topic/agent/knowledge/" + event.getJobId(), event);
            }
        } catch (Exception ignored) {
            // 广播失败不应影响任务执行。
        }
    }
}
