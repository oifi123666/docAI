package com.javaee.aiservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks bucket access by permission group without changing file operation flows.
 */
@Service
public class BucketPermissionService {

    private final RequestUserContext requestUserContext;

    @Value("${minio.bucket-permissions:}")
    private String bucketPermissions;

    @Value("${minio.bucket-permission-default-allow:false}")
    private boolean defaultAllow;

    public BucketPermissionService(RequestUserContext requestUserContext) {
        this.requestUserContext = requestUserContext;
    }

    public void assertCanAccess(String bucketName) {
        if (requestUserContext.isAdmin()) {
            return;
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new SecurityException("存储桶名称不能为空");
        }
        if (isAllowed(bucketName, requestUserContext.getCurrentPermissionGroups())) {
            return;
        }
        throw new SecurityException("无权访问存储桶: " + bucketName);
    }

    boolean isAllowed(String bucketName, Set<String> userGroups) {
        if (bucketPermissions == null || bucketPermissions.isBlank()) {
            return defaultAllow;
        }
        Set<String> allowedGroups = parseGroups(permissionsForBucket(bucketName));
        if (allowedGroups.isEmpty()) {
            return defaultAllow;
        }
        Set<String> normalizedUserGroups = normalizeGroups(userGroups);
        return normalizedUserGroups.contains("*") || allowedGroups.contains("*")
                || allowedGroups.stream().anyMatch(normalizedUserGroups::contains);
    }

    private Set<String> parseGroups(String groups) {
        if (groups == null || groups.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(groups.split("[,;]"))
                .map(this::normalizeGroup)
                .filter(group -> !group.isBlank())
                .collect(Collectors.toSet());
    }

    private String permissionsForBucket(String bucketName) {
        String normalizedBucket = normalizeGroup(bucketName);
        for (String entry : bucketPermissions.split("[;\\n]")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && normalizedBucket.equals(normalizeGroup(parts[0]))) {
                return parts[1];
            }
        }
        return "";
    }

    private Set<String> normalizeGroups(Set<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String group : groups) {
            String value = normalizeGroup(group);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String normalizeGroup(String group) {
        return group == null ? "" : group.trim().toLowerCase(Locale.ROOT);
    }
}
