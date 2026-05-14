package com.javaee.common.utils;

import com.javaee.common.constant.CommonConstant;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 * @author qxk
 * @description: 文件工具（大小/格式/路径）
 */
public class FileUtils {

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList("doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx");
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv");
    private static final List<String> AUDIO_EXTENSIONS = Arrays.asList("mp3", "wav", "flac", "aac", "ogg");
    private static final List<String> ARCHIVE_EXTENSIONS = Arrays.asList("zip", "rar", "7z", "tar", "gz");

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#.##");

    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 扩展名
     */
    public static String getExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(CommonConstant.PERIOD);
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 判断是否为图片文件
     * @param filename 文件名
     * @return 是否为图片
     */
    public static boolean isImage(String filename) {
        String extension = getExtension(filename);
        return IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * 判断是否为文档文件
     * @param filename 文件名
     * @return 是否为文档
     */
    public static boolean isDocument(String filename) {
        String extension = getExtension(filename);
        return DOCUMENT_EXTENSIONS.contains(extension);
    }

    /**
     * 判断是否为视频文件
     * @param filename 文件名
     * @return 是否为视频
     */
    public static boolean isVideo(String filename) {
        String extension = getExtension(filename);
        return VIDEO_EXTENSIONS.contains(extension);
    }

    /**
     * 判断是否为音频文件
     * @param filename 文件名
     * @return 是否为音频
     */
    public static boolean isAudio(String filename) {
        String extension = getExtension(filename);
        return AUDIO_EXTENSIONS.contains(extension);
    }

    /**
     * 判断是否为压缩文件
     * @param filename 文件名
     * @return 是否为压缩文件
     */
    public static boolean isArchive(String filename) {
        String extension = getExtension(filename);
        return ARCHIVE_EXTENSIONS.contains(extension);
    }

    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的字符串
     */
    public static String formatSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return SIZE_FORMAT.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * 获取文件大小
     * @param filepath 文件路径
     * @return 文件大小
     */
    public static long getFileSize(String filepath) {
        File file = new File(filepath);
        if (!file.exists()) {
            return 0;
        }
        return file.length();
    }

    /**
     * 判断文件是否存在
     * @param filepath 文件路径
     * @return 是否存在
     */
    public static boolean exists(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return false;
        }
        return new File(filepath).exists();
    }

    /**
     * 判断是否为文件
     * @param filepath 文件路径
     * @return 是否为文件
     */
    public static boolean isFile(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return false;
        }
        File file = new File(filepath);
        return file.exists() && file.isFile();
    }

    /**
     * 判断是否为目录
     * @param filepath 文件路径
     * @return 是否为目录
     */
    public static boolean isDirectory(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return false;
        }
        File file = new File(filepath);
        return file.exists() && file.isDirectory();
    }

    /**
     * 创建目录
     * @param dirpath 目录路径
     * @return 是否创建成功
     */
    public static boolean createDirectory(String dirpath) {
        if (StringUtils.isEmpty(dirpath)) {
            return false;
        }
        File dir = new File(dirpath);
        if (dir.exists()) {
            return true;
        }
        return dir.mkdirs();
    }

    /**
     * 删除文件
     * @param filepath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return false;
        }
        File file = new File(filepath);
        if (!file.exists()) {
            return true;
        }
        return file.delete();
    }

    /**
     * 获取文件名（不含扩展名）
     * @param filename 文件名
     * @return 文件名（不含扩展名）
     */
    public static String getFileNameWithoutExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(CommonConstant.PERIOD);
        if (lastDotIndex == -1) {
            return filename;
        }
        return filename.substring(0, lastDotIndex);
    }

    /**
     * 获取文件名（含扩展名）
     * @param filepath 文件路径
     * @return 文件名
     */
    public static String getFileName(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return "";
        }
        int lastSeparatorIndex = Math.max(filepath.lastIndexOf(CommonConstant.FILE_SEPARATOR), filepath.lastIndexOf("/"));
        if (lastSeparatorIndex == -1) {
            return filepath;
        }
        return filepath.substring(lastSeparatorIndex + 1);
    }

    /**
     * 获取文件父目录
     * @param filepath 文件路径
     * @return 父目录路径
     */
    public static String getParentPath(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return "";
        }
        File File = new File(filepath);
        return File.getParent();
    }

    /**
     * 拼接路径
     * @param paths 路径片段
     * @return 拼接后的路径
     */
    public static String joinPath(String... paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            String path = paths[i];
            if (StringUtils.isEmpty(path)) {
                continue;
            }
            if (!result.toString().endsWith(CommonConstant.FILE_SEPARATOR) && !path.startsWith(CommonConstant.FILE_SEPARATOR)) {
                result.append(CommonConstant.FILE_SEPARATOR);
            }
            result.append(path);
        }
        return result.toString();
    }

    /**
     * 规范化路径
     * @param path 路径
     * @return 规范化后的路径
     */
    public static String normalizePath(String path) {
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        try {
            Path normalizedPath = Paths.get(path).normalize();
            return normalizedPath.toString();
        } catch (Exception e) {
            return path;
        }
    }

    /**
     * 获取文件MIME类型
     * @param filename 文件名
     * @return MIME类型
     */
    public static String getMimeType(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return "application/octet-stream";
        }
        String extension = getExtension(filename);
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "html":
                return "text/html";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            case "zip":
                return "application/zip";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 读取文件内容
     * @param filepath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    public static String readFile(String filepath) throws IOException {
        if (!exists(filepath)) {
            return "";
        }
        Path path = Paths.get(filepath);
        return Files.readString(path);
    }

    /**
     * 写入文件内容
     * @param filepath 文件路径
     * @param content 文件内容
     * @throws IOException IO异常
     */
    public static void writeFile(String filepath, String content) throws IOException {
        Path path = Paths.get(filepath);
        Files.writeString(path, content);
    }
}
