package com.javaee.documentservice.controller;

import com.javaee.common.model.Result;
import com.javaee.documentservice.dto.AnnotationCreateDTO;
import com.javaee.documentservice.service.AnnotationService;
import com.javaee.documentservice.vo.AnnotationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/annotations")
@Tag(name = "文档批注", description = "文档批注管理接口")
public class AnnotationController {

    @Autowired
    private AnnotationService annotationService;

    @PostMapping
    @Operation(summary = "添加批注", description = "为文档添加批注")
    public Result<AnnotationVO> createAnnotation(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @RequestBody AnnotationCreateDTO dto) {
        Long userId = 1L;
        dto.setDocumentId(documentId);
        AnnotationVO vo = annotationService.createAnnotation(dto, userId);
        return Result.success(vo);
    }

    @GetMapping
    @Operation(summary = "获取批注列表", description = "获取文档的所有批注")
    public Result<List<AnnotationVO>> getAnnotations(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        List<AnnotationVO> annotations = annotationService.getAnnotationsByDocumentId(documentId);
        return Result.success(annotations);
    }

    @GetMapping("/{annotationId}")
    @Operation(summary = "获取批注详情", description = "获取指定批注的详情")
    public Result<AnnotationVO> getAnnotation(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "批注ID") @PathVariable String annotationId) {
        AnnotationVO vo = annotationService.getAnnotationById(annotationId);
        return Result.success(vo);
    }

    @PutMapping("/{annotationId}")
    @Operation(summary = "更新批注", description = "更新指定批注")
    public Result<Void> updateAnnotation(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "批注ID") @PathVariable String annotationId,
            @RequestBody AnnotationCreateDTO dto) {
        Long userId = 1L;
        annotationService.updateAnnotation(annotationId, dto, userId);
        return Result.success();
    }

    @DeleteMapping("/{annotationId}")
    @Operation(summary = "删除批注", description = "删除指定批注")
    public Result<Void> deleteAnnotation(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "批注ID") @PathVariable String annotationId) {
        Long userId = 1L;
        annotationService.deleteAnnotation(annotationId, userId);
        return Result.success();
    }

    @GetMapping("/line/{lineNumber}")
    @Operation(summary = "获取指定行的批注", description = "获取文档指定行的所有批注")
    public Result<List<AnnotationVO>> getAnnotationsByLine(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "行号") @PathVariable Integer lineNumber) {
        List<AnnotationVO> annotations = annotationService.getAnnotationsByLineNumber(documentId, lineNumber);
        return Result.success(annotations);
    }
}
