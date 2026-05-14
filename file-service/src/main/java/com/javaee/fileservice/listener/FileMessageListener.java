package com.javaee.fileservice.listener;

import com.javaee.fileservice.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件服务消息监听器
 */
@Slf4j
@Component
public class FileMessageListener {

    /**
     * 处理文件上传消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_UPLOAD_QUEUE)
    public void handleFileUploadMessage(Map<String, Object> message) {
        log.info("=== 收到文件上传消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        String fileId = (String) message.get("fileId");
        String fileName = (String) message.get("fileName");
        Long fileSize = message.get("fileSize") instanceof Integer ? 
            ((Integer) message.get("fileSize")).longValue() : (Long) message.get("fileSize");
        
        log.info("文件ID: {}, 文件名: {}, 文件大小: {} bytes", fileId, fileName, fileSize);
        
        log.info("文件上传消息处理完成");
    }

    /**
     * 处理文件下载消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_DOWNLOAD_QUEUE)
    public void handleFileDownloadMessage(Map<String, Object> message) {
        log.info("=== 收到文件下载消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        String fileId = (String) message.get("fileId");
        String userId = (String) message.get("userId");
        
        log.info("文件ID: {}, 用户ID: {}", fileId, userId);
        
        log.info("文件下载消息处理完成");
    }

    /**
     * 处理文件处理消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_PROCESS_QUEUE)
    public void handleFileProcessMessage(Map<String, Object> message) {
        log.info("=== 收到文件处理消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        String fileId = (String) message.get("fileId");
        String processType = (String) message.get("processType");
        
        log.info("文件ID: {}, 处理类型: {}", fileId, processType);
        
        log.info("文件处理消息处理完成");
    }

    /**
     * 处理文件删除消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_DELETE_QUEUE)
    public void handleFileDeleteMessage(Map<String, Object> message) {
        log.info("=== 收到文件删除消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        String fileId = (String) message.get("fileId");
        String userId = (String) message.get("userId");
        
        log.info("文件ID: {}, 用户ID: {}", fileId, userId);
        
        log.info("文件删除消息处理完成");
    }
}
