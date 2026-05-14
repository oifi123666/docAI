package com.javaee.fileservice.enums;

/**
 * 文件类型枚举
 */
public enum FileTypeEnum {

    IMAGE("image", "图片文件"),
    DOCUMENT("document", "文档文件"),
    VIDEO("video", "视频文件"),
    AUDIO("audio", "音频文件"),
    ARCHIVE("archive", "压缩文件"),
    OTHER("other", "其他文件");

    private final String code;
    private final String desc;

    FileTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static FileTypeEnum getByCode(String code) {
        for (FileTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }

}
