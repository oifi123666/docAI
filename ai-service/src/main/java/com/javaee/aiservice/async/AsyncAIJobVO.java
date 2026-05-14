package com.javaee.aiservice.async;

import lombok.Data;

import java.io.Serializable;

/**
 * Redis-persisted status for an asynchronous AI job.
 */
@Data
public class AsyncAIJobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String type;
    private String model;
    private String userId;
    private String status;
    private Long submittedAt;
    private Long startedAt;
    private Long finishedAt;
    private Object result;
    private String error;
}
