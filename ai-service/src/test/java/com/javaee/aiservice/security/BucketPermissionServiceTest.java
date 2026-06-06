package com.javaee.aiservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BucketPermissionServiceTest {

    @Test
    void userGroupCanAccessDocAiBucket() {
        RequestUserContext requestUserContext = mock(RequestUserContext.class);
        when(requestUserContext.isAdmin()).thenReturn(false);
        when(requestUserContext.getCurrentPermissionGroups()).thenReturn(Set.of("user"));

        BucketPermissionService service = new BucketPermissionService(requestUserContext);
        ReflectionTestUtils.setField(service, "bucketPermissions", "documents=admin,user;doc-ai=admin,user");
        ReflectionTestUtils.setField(service, "defaultAllow", false);

        assertThatCode(() -> service.assertCanAccess("doc-ai")).doesNotThrowAnyException();
    }
}
