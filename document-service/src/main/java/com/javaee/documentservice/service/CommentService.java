package com.javaee.documentservice.service;

import com.javaee.documentservice.dto.CommentCreateDTO;
import com.javaee.documentservice.vo.CommentVO;

import java.util.List;

public interface CommentService {

    CommentVO createComment(CommentCreateDTO dto, Long userId);

    List<CommentVO> getCommentsByDocumentId(String documentId);

    CommentVO getCommentById(String id);

    void deleteComment(String id, Long userId);

    List<CommentVO> getReplies(String parentId);
}
