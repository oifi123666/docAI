package com.javaee.aiservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;
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
        if (isAnonymous(authentication) || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        String principal = authentication.getPrincipal().toString();
        if ("anonymousUser".equalsIgnoreCase(principal) || "anonymous".equalsIgnoreCase(principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymous(authentication) || authentication.getAuthorities() == null) {
            return "user";
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.isBlank())
                .findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .orElse("user");
    }

    public Set<String> getCurrentPermissionGroups() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymous(authentication) || authentication.getAuthorities() == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.isBlank())
                .map(authority -> authority.replaceFirst("^ROLE_", "").replaceFirst("^GROUP_", ""))
                .collect(Collectors.toSet());
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getCurrentRole());
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }
}
