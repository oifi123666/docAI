package com.javaee.documentservice.service.impl;

import com.javaee.common.exception.BusinessException;
import com.javaee.documentservice.dto.CommentCreateDTO;
import com.javaee.documentservice.entity.Document;
import com.javaee.documentservice.entity.DocumentComment;
import com.javaee.documentservice.mapper.DocumentCommentMapper;
import com.javaee.documentservice.mapper.DocumentMapper;
import com.javaee.documentservice.service.CommentService;
import com.javaee.documentservice.vo.CommentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Autowired
    private DocumentCommentMapper commentMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Override
    @Transactional
    public CommentVO createComment(CommentCreateDTO dto, Long userId) {
        Document document = documentMapper.selectById(dto.getDocumentId());
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        if (dto.getParentId() != null) {
            DocumentComment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("父评论不存在");
            }
        }

        DocumentComment comment = new DocumentComment();
        comment.setDocumentId(dto.getDocumentId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        comment.setCreatedBy(String.valueOf(userId));
        comment.setCreateTime(LocalDateTime.now());
        comment.setStatus("active");

        commentMapper.insert(comment);
        log.info("创建评论成功: id={}, documentId={}", comment.getId(), dto.getDocumentId());

        return convertToVO(comment);
    }

    @Override
    public List<CommentVO> getCommentsByDocumentId(String documentId) {
        List<DocumentComment> rootComments = commentMapper.selectRootCommentsByDocumentId(documentId);
        return rootComments.stream()
                .map(this::convertToVOWithReplies)
                .collect(Collectors.toList());
    }

    @Override
    public CommentVO getCommentById(String id) {
        DocumentComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return convertToVO(comment);
    }

    @Override
    @Transactional
    public void deleteComment(String id, Long userId) {
        DocumentComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此评论");
        }

        comment.setStatus("deleted");
        commentMapper.updateById(comment);
        log.info("删除评论成功: id={}", id);
    }

    @Override
    public List<CommentVO> getReplies(String parentId) {
        List<DocumentComment> replies = commentMapper.selectByParentId(parentId);
        return replies.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private CommentVO convertToVO(DocumentComment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setDocumentId(comment.getDocumentId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setCreatedBy(comment.getCreatedBy());
        vo.setCreateTime(comment.getCreateTime());
        vo.setStatus(comment.getStatus());
        vo.setReplies(new ArrayList<>());
        return vo;
    }

    private CommentVO convertToVOWithReplies(DocumentComment comment) {
        CommentVO vo = convertToVO(comment);
        List<DocumentComment> replies = commentMapper.selectByParentId(comment.getId());
        vo.setReplies(replies.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return vo;
    }
}
