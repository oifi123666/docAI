package com.javaee.documentservice.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {
    private String documentId;
    private String content;
    private String parentId;
}
