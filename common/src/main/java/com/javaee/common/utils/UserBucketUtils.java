package com.javaee.common.utils;

import java.util.Locale;

/**
 * Builds stable MinIO bucket names for user-owned storage.
 */
public final class UserBucketUtils {

    private static final String USER_BUCKET_PREFIX = "user-";

    private UserBucketUtils() {
    }

    public static String bucketNameForUser(Object userId) {
        if (userId == null || userId.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        String normalized = userId.toString().trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.-]", "-")
                .replaceAll("\\.+", ".")
                .replaceAll("-+", "-")
                .replaceAll("^[.-]+", "")
                .replaceAll("[.-]+$", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能生成有效存储桶名称");
        }
        String bucketName = USER_BUCKET_PREFIX + normalized;
        if (bucketName.length() > 63) {
            bucketName = bucketName.substring(0, 63).replaceAll("[.-]+$", "");
        }
        if (bucketName.length() < 3) {
            bucketName = (bucketName + "-bucket").substring(0, 3);
        }
        return bucketName;
    }

    public static boolean isUserBucket(String bucketName, Object userId) {
        if (bucketName == null || userId == null) {
            return false;
        }
        return bucketNameForUser(userId).equals(bucketName);
    }
}
