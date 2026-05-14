package com.javaee.aiservice.async;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Message sent to RabbitMQ for asynchronous model execution.
 */
@Data
public class AsyncAIJobMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String type;
    private String model;
    private String userId;
    private Long createdAt;
    private Map<String, Object> payload = new HashMap<>();
}
