package com.javaee.documentservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private String id;
    private String documentId;
    private Long userId;
    private String userName;
    private String content;
    private String parentId;
    private String createdBy;
    private LocalDateTime createTime;
    private String status;
    private List<CommentVO> replies;
}
