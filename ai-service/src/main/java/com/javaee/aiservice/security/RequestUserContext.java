package com.javaee.aiservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads the authenticated user from Spring Security.
 */
@Component
public class RequestUserContext {

    public String getRequiredUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new SecurityException("用户未认证，请先登录"));
    }

    public Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        return Optional.of(authentication.getPrincipal().toString());
    }

    public String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return "user";
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.isBlank())
                .findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .orElse("user");
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getCurrentRole());
    }
}
