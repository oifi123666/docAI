package com.javaee.fileservice.enums;

/**
 * 存储类型枚举
 */
public enum StorageTypeEnum {

    LOCAL("local", "本地存储"),
    MINIO("minio", "MinIO存储"),
    S3("s3", "AWS S3存储");

    private final String code;
    private final String desc;

    StorageTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static StorageTypeEnum getByCode(String code) {
        for (StorageTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return LOCAL;
    }

}
