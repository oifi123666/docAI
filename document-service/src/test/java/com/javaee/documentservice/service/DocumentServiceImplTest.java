package com.javaee.documentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.documentservice.dto.DocumentCreateDTO;
import com.javaee.documentservice.dto.DocumentUpdateDTO;
import com.javaee.documentservice.entity.Document;
import com.javaee.documentservice.mapper.DocumentMapper;
import com.javaee.documentservice.mapper.DocumentVersionMapper;
import com.javaee.documentservice.service.impl.DocumentServiceImpl;
import com.javaee.documentservice.vo.DocumentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceImplTest {

    private DocumentMapper documentMapper;
    private DocumentVersionMapper documentVersionMapper;
    private DocumentContentService documentContentService;
    private DocumentAccessService documentAccessService;
    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentMapper = mock(DocumentMapper.class);
        documentVersionMapper = mock(DocumentVersionMapper.class);
        documentContentService = mock(DocumentContentService.class);
        documentAccessService = mock(DocumentAccessService.class);
        documentService = new DocumentServiceImpl();
        ReflectionTestUtils.setField(documentService, "documentMapper", documentMapper);
        ReflectionTestUtils.setField(documentService, "documentVersionMapper", documentVersionMapper);
        ReflectionTestUtils.setField(documentService, "documentContentService", documentContentService);
        ReflectionTestUtils.setField(documentService, "documentAccessService", documentAccessService);
        ReflectionTestUtils.setField(documentService, "objectMapper", new ObjectMapper());
    }

    @Test
    void createStoresDocumentInUserBucketAndGrantsOwnerAccess() {
        when(documentContentService.getBucketName(7L)).thenReturn("user-7");
        when(documentContentService.getObjectName("doc-1")).thenReturn("document-content/doc-1.txt");
        doAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId("doc-1");
            return 1;
        }).when(documentMapper).insert(any(Document.class));

        DocumentCreateDTO dto = new DocumentCreateDTO();
        dto.setTitle("计划");
        dto.setCategory("work");
        dto.setTags(List.of("q2"));

        DocumentVO vo = documentService.create(dto, 7L);

        ArgumentCaptor<Document> inserted = ArgumentCaptor.forClass(Document.class);
        verify(documentMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getBucketName()).isEqualTo("user-7");
        verify(documentMapper).updateById(any(Document.class));
        verify(documentAccessService).grantOwnerAccess("doc-1", "user-7", 7L);
        assertThat(vo.getBucketName()).isEqualTo("user-7");
        assertThat(vo.getObjectName()).isEqualTo("document-content/doc-1.txt");
    }

    @Test
    void collaboratorReadsContentFromDocumentBucket() {
        Document document = document("doc-2", 1L, "team-bucket", "document-content/doc-2.txt");
        when(documentMapper.selectById("doc-2")).thenReturn(document);
        doNothing().when(documentAccessService).assertCanRead(document, 2L);
        when(documentContentService.getContent("doc-2", "team-bucket")).thenReturn("shared text");

        DocumentVO vo = documentService.getById("doc-2", 2L);

        verify(documentAccessService).assertCanRead(document, 2L);
        verify(documentContentService).getContent("doc-2", "team-bucket");
        assertThat(vo.getContent()).isEqualTo("shared text");
        assertThat(vo.getBucketName()).isEqualTo("team-bucket");
    }

    @Test
    void grantAccessUsesDocumentBucketAndRequiresOwner() {
        Document document = document("doc-3", 1L, "user-1", "document-content/doc-3.txt");
        when(documentMapper.selectById("doc-3")).thenReturn(document);
        when(documentAccessService.isOwner(document, 1L)).thenReturn(true);

        documentService.grantAccess("doc-3", 2L, "editor", 1L);

        verify(documentAccessService).grantAccess("doc-3", "user-1", 2L, "editor");
    }

    @Test
    void collaboratorCanSaveEmptyContentWithoutOverwritingMetadata() {
        Document document = document("doc-4", 1L, "owner-bucket", "document-content/doc-4.txt");
        when(documentMapper.selectById("doc-4")).thenReturn(document);
        doNothing().when(documentAccessService).assertCanWrite(document, 2L);
        when(documentContentService.getContent("doc-4", "owner-bucket")).thenReturn("before");

        DocumentUpdateDTO dto = new DocumentUpdateDTO();
        dto.setContent("");

        DocumentVO vo = documentService.update("doc-4", dto, 2L);

        verify(documentAccessService).assertCanWrite(document, 2L);
        verify(documentContentService).updateContent("doc-4", "owner-bucket", "");
        assertThat(document.getTitle()).isEqualTo("doc");
        assertThat(vo.getContent()).isEqualTo("");
    }

    private Document document(String id, Long userId, String bucketName, String objectName) {
        Document document = new Document();
        document.setId(id);
        document.setTitle("doc");
        document.setUserId(userId);
        document.setBucketName(bucketName);
        document.setObjectName(objectName);
        document.setStatus("active");
        document.setVersion(1);
        document.setTags("[]");
        document.setKeywords("[]");
        return document;
    }
}
