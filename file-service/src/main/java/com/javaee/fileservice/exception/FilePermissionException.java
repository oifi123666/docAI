package com.javaee.fileservice.exception;

/**
 * 文件权限异常
 */
public class FilePermissionException extends RuntimeException {

    public FilePermissionException(String message) {
        super(message);
    }

    public FilePermissionException(String message, Throwable cause) {
        super(message, cause);
    }

}
