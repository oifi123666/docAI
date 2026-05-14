package com.javaee.common.constant;

/**
 * @author qxk
 * @description: 通用常量（分页/正则）
 */
public class CommonConstant {
    // 分页常量
    public static final Integer PAGE_SIZE = 10;
    public static final Integer PAGE_NUM = 1;
    public static final Integer MAX_PAGE_SIZE = 100;

    // 正则表达式常量
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    // 字符常量
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String SEMICOLON = ";";
    public static final String PERIOD = ".";
    public static final String HYPHEN = "-";
    public static final String UNDERSCORE = "_";
    public static final String SPACE = " ";
    public static final String NEWLINE = "\\n";
    public static final String CARRIAGE_RETURN = "\\r";

    // 日期格式常量
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    // HTTP常量
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HTTP_CONTENT_TYPE_JSON = "application/json";
    public static final String HTTP_CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    // 业务常量
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final String DEFAULT_LANGUAGE = "zh-CN";
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    // 文件常量
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    // 安全常量
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String REFRESH_TOKEN_PREFIX = "Refresh ";
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_CLAIM_USER_ID = "userId";
    public static final String TOKEN_CLAIM_USERNAME = "username";
    public static final String TOKEN_CLAIM_ROLE = "role";
}

