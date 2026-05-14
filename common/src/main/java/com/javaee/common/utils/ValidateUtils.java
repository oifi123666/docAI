package com.javaee.common.utils;

import com.javaee.common.constant.CommonConstant;
import com.javaee.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author qxk
 * @description: 参数校验工具（非空/格式）
 */
public class ValidateUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(CommonConstant.EMAIL_REGEX);
    private static final Pattern PHONE_PATTERN = Pattern.compile(CommonConstant.PHONE_REGEX);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(CommonConstant.PASSWORD_REGEX);

    /**
     * 校验字符串非空
     * @param str 待校验字符串
     * @param message 错误消息
     */
    public static void notEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验字符串非空
     * @param str 待校验字符串
     */
    public static void notEmpty(String str) {
        notEmpty(str, "参数不能为空");
    }

    /**
     * 校验对象非空
     * @param obj 待校验对象
     * @param message 错误消息
     */
    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验对象非空
     * @param obj 待校验对象
     */
    public static void notNull(Object obj) {
        notNull(obj, "参数不能为空");
    }

    /**
     * 校验集合非空
     * @param collection 待校验集合
     * @param message 错误消息
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验集合非空
     * @param collection 待校验集合
     */
    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection, "集合不能为空");
    }

    /**
     * 校验Map非空
     * @param map 待校验Map
     * @param message 错误消息
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验Map非空
     * @param map 待校验Map
     */
    public static void notEmpty(Map<?, ?> map) {
        notEmpty(map, "Map不能为空");
    }

    /**
     * 校验数组非空
     * @param array 待校验数组
     * @param message 错误消息
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验数组非空
     * @param array 待校验数组
     */
    public static void notEmpty(Object[] array) {
        notEmpty(array, "数组不能为空");
    }

    /**
     * 校验邮箱格式
     * @param email 待校验邮箱
     * @param message 错误消息
     */
    public static void email(String email, String message) {
        notEmpty(email, message);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验邮箱格式
     * @param email 待校验邮箱
     */
    public static void email(String email) {
        email(email, "邮箱格式不正确");
    }

    /**
     * 校验手机号格式
     * @param phone 待校验手机号
     * @param message 错误消息
     */
    public static void phone(String phone, String message) {
        notEmpty(phone, message);
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验手机号格式
     * @param phone 待校验手机号
     */
    public static void phone(String phone) {
        phone(phone, "手机号格式不正确");
    }

    /**
     * 校验密码格式
     * @param password 待校验密码
     * @param message 错误消息
     */
    public static void password(String password, String message) {
        notEmpty(password, message);
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验密码格式
     * @param password 待校验密码
     */
    public static void password(String password) {
        password(password, "密码格式不正确，必须包含大小写字母、数字和特殊字符，且长度至少8位");
    }

    /**
     * 校验字符串长度
     * @param str 待校验字符串
     * @param min 最小长度
     * @param max 最大长度
     * @param message 错误消息
     */
    public static void length(String str, int min, int max, String message) {
        notEmpty(str, message);
        if (str.length() < min || str.length() > max) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验字符串长度
     * @param str 待校验字符串
     * @param min 最小长度
     * @param max 最大长度
     */
    public static void length(String str, int min, int max) {
        length(str, min, max, "字符串长度必须在" + min + "到" + max + "之间");
    }

    /**
     * 校验数字范围
     * @param num 待校验数字
     * @param min 最小值
     * @param max 最大值
     * @param message 错误消息
     */
    public static void range(Integer num, int min, int max, String message) {
        notNull(num, message);
        if (num < min || num > max) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验数字范围
     * @param num 待校验数字
     * @param min 最小值
     * @param max 最大值
     */
    public static void range(Integer num, int min, int max) {
        range(num, min, max, "数值必须在" + min + "到" + max + "之间");
    }

    /**
     * 校验布尔表达式
     * @param condition 布尔表达式
     * @param message 错误消息
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验布尔表达式
     * @param condition 布尔表达式
     */
    public static void isTrue(boolean condition) {
        isTrue(condition, "条件不满足");
    }
}
