package com.javaee.documentservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.documentservice.collaborate.CollaborateMessage;
import com.javaee.documentservice.collaborate.EditOperation;
import com.javaee.documentservice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class CollaborateMessageListener {

    private static final Logger log = LoggerFactory.getLogger(CollaborateMessageListener.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.COLLABORATE_EDIT_QUEUE)
    public void handleCollaborateEdit(byte[] message) {
        try {
            EditOperation operation = objectMapper.readValue(message, EditOperation.class);
            log.debug("收到跨实例编辑操作: opId={}, documentId={}, userId={}",
                    operation.getOpId(), operation.getDocumentId(), operation.getUserId());

            CollaborateMessage editMsg = CollaborateMessage.edit(operation);
            messagingTemplate.convertAndSend("/topic/doc/" + operation.getDocumentId(), editMsg);
        } catch (Exception e) {
            log.error("处理跨实例协同编辑消息失败", e);
        }
    }
}