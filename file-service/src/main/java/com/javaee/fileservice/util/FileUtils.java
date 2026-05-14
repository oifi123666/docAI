package com.javaee.fileservice.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件处理工具类
 */
public class FileUtils {

    /**
     * 生成唯一文件名
     */
    public static String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String fileName = UUID.randomUUID().toString();
        if (extension != null) {
            fileName += "." + extension;
        }
        return fileName;
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 获取文件类型
     */
    public static String getFileType(String fileName) {
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return "other";
        }

        // 图片文件
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
        for (String ext : imageExtensions) {
            if (ext.equals(extension)) {
                return "image";
            }
        }

        // 文档文件
        String[] docExtensions = {"doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx"};
        for (String ext : docExtensions) {
            if (ext.equals(extension)) {
                return "document";
            }
        }

        // 视频文件
        String[] videoExtensions = {"mp4", "avi", "mov", "wmv", "flv", "mkv"};
        for (String ext : videoExtensions) {
            if (ext.equals(extension)) {
                return "video";
            }
        }

        // 音频文件
        String[] audioExtensions = {"mp3", "wav", "ogg", "flac", "aac"};
        for (String ext : audioExtensions) {
            if (ext.equals(extension)) {
                return "audio";
            }
        }

        // 压缩文件
        String[] archiveExtensions = {"zip", "rar", "7z", "tar", "gz"};
        for (String ext : archiveExtensions) {
            if (ext.equals(extension)) {
                return "archive";
            }
        }

        return "other";
    }

    /**
     * 保存上传的文件到本地
     */
    public static File saveMultipartFile(MultipartFile file, String filePath) throws IOException {
        File destFile = new File(filePath);
        File parentDir = destFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        file.transferTo(destFile);
        return destFile;
    }

    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    /**
     * 检查文件大小是否超限
     */
    public static boolean checkFileSize(MultipartFile file, long maxSize) {
        return file.getSize() <= maxSize * 1024 * 1024;
    }

    /**
     * 将字节数组转换为输入流
     */
    public static java.io.InputStream toInputStream(byte[] bytes) {
        return new java.io.ByteArrayInputStream(bytes);
    }

    /**
     * 将输入流转换为字节数组
     */
    public static byte[] toByteArray(java.io.InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    /**
     * 根据文件名获取内容类型
     */
    public static String getContentType(String fileName) {
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return "application/octet-stream";
        }

        // 常见文件类型的MIME类型映射
        java.util.Map<String, String> mimeTypes = new java.util.HashMap<>();
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("webp", "image/webp");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypes.put("xls", "application/vnd.ms-excel");
        mimeTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
        mimeTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("rar", "application/x-rar-compressed");
        mimeTypes.put("7z", "application/x-7z-compressed");
        mimeTypes.put("mp3", "audio/mpeg");
        mimeTypes.put("wav", "audio/wav");
        mimeTypes.put("mp4", "video/mp4");
        mimeTypes.put("avi", "video/x-msvideo");
        mimeTypes.put("mov", "video/quicktime");

        return mimeTypes.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

}
