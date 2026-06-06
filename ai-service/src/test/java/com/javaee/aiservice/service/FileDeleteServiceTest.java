package com.javaee.aiservice.service;

import com.javaee.aiservice.client.DocumentServiceClient;
import com.javaee.aiservice.dto.FileDeleteDTO;
import com.javaee.aiservice.security.BucketPermissionService;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.vo.FileDeleteVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileDeleteServiceTest {

    private RecycleBinService recycleBinService;
    private MinIOService minIOService;
    private BucketPermissionService bucketPermissionService;
    private RequestUserContext requestUserContext;
    private DocumentServiceClient documentServiceClient;
    private FileDeleteService fileDeleteService;

    @BeforeEach
    void setUp() {
        recycleBinService = mock(RecycleBinService.class);
        minIOService = mock(MinIOService.class);
        bucketPermissionService = mock(BucketPermissionService.class);
        requestUserContext = mock(RequestUserContext.class);
        documentServiceClient = mock(DocumentServiceClient.class);
        fileDeleteService = new FileDeleteService();
        ReflectionTestUtils.setField(fileDeleteService, "recycleBinService", recycleBinService);
        ReflectionTestUtils.setField(fileDeleteService, "minIOService", minIOService);
        ReflectionTestUtils.setField(fileDeleteService, "bucketPermissionService", bucketPermissionService);
        ReflectionTestUtils.setField(fileDeleteService, "requestUserContext", requestUserContext);
        ReflectionTestUtils.setField(fileDeleteService, "documentServiceClient", documentServiceClient);
    }

    @Test
    void deleteFileDeletesDocumentThroughDocumentService() {
        FileDeleteDTO dto = new FileDeleteDTO();
        dto.setDocumentId("doc-1");
        dto.setRequireConfirmation(false);

        FileDeleteVO vo = fileDeleteService.deleteFile(dto, "agent-approved:1");

        verify(documentServiceClient).deleteDocument("doc-1");
        verify(bucketPermissionService, never()).assertCanAccess("user-1");
        verifyNoInteractions(minIOService);
        verifyNoInteractions(recycleBinService);
        assertThat(vo.getRecycleId()).isNull();
        assertThat(vo.getStatus()).isEqualTo("deleted");
        assertThat(vo.getMessage()).isEqualTo("文档已永久删除");
    }

    @Test
    void deleteFileDefaultsDirectObjectDeleteToCurrentUserBucket() throws Exception {
        FileDeleteDTO dto = new FileDeleteDTO();
        dto.setObjectName("report.txt");
        dto.setRequireConfirmation(false);
        when(requestUserContext.getRequiredUserId()).thenReturn("9");

        FileDeleteVO vo = fileDeleteService.deleteFile(dto, "agent-approved:9");

        verify(bucketPermissionService).assertCanAccess("user-9");
        verify(minIOService).deleteFile("user-9", "report.txt");
        verifyNoInteractions(recycleBinService);
        assertThat(vo.getRecycleId()).isNull();
        assertThat(vo.getStatus()).isEqualTo("deleted");
        assertThat(vo.getMessage()).isEqualTo("文件已永久删除");
    }
}
