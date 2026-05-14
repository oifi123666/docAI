package com.javaee.fileservice.util;

import java.io.File;

/**
 * 路径拼接和校验工具类
 */
public class PathUtils {

    /**
     * 拼接路径
     */
    public static String joinPath(String... paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (path == null || path.isEmpty()) {
                continue;
            }

            if (i > 0 && !path.startsWith(File.separator)) {
                sb.append(File.separator);
            }

            if (i == paths.length - 1 && path.endsWith(File.separator)) {
                path = path.substring(0, path.length() - 1);
            }

            sb.append(path);
        }

        return sb.toString();
    }

    /**
     * 标准化路径
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        // 替换反斜杠为正斜杠
        path = path.replace('\\', '/');

        // 移除多余的斜杠
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }

        // 移除末尾的斜杠
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * 检查路径是否安全（防止目录遍历攻击）
     */
    public static boolean isSafePath(String path) {
        if (path == null) {
            return false;
        }

        // 检查是否包含 ".." 路径
        if (path.contains("..")) {
            return false;
        }

        // 检查是否以根路径开头（绝对路径）
        if (path.startsWith("/") || path.startsWith(File.separator)) {
            return false;
        }

        // 检查是否包含 Windows 驱动器号
        if (path.matches("^[a-zA-Z]:.*")) {
            return false;
        }

        return true;
    }

    /**
     * 获取相对路径
     */
    public static String getRelativePath(String basePath, String fullPath) {
        if (basePath == null || fullPath == null) {
            return fullPath;
        }

        basePath = normalizePath(basePath);
        fullPath = normalizePath(fullPath);

        if (fullPath.startsWith(basePath)) {
            String relativePath = fullPath.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }

        return fullPath;
    }

}
