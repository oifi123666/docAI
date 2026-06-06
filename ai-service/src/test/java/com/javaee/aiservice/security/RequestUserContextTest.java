package com.javaee.aiservice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestUserContextTest {

    private final RequestUserContext requestUserContext = new RequestUserContext();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousAuthenticationIsNotATenantUser() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(requestUserContext.getCurrentUserId()).isEmpty();
        assertThat(requestUserContext.getCurrentPermissionGroups()).isEmpty();
        assertThatThrownBy(requestUserContext::getRequiredUserId)
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("用户未认证");
    }

    @Test
    void authenticatedUserKeepsRoleAndGroups() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user-1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_user"), new SimpleGrantedAuthority("GROUP_ops"))));

        assertThat(requestUserContext.getRequiredUserId()).isEqualTo("user-1");
        assertThat(requestUserContext.getCurrentPermissionGroups()).contains("user", "ops");
    }
}
