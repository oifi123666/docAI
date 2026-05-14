package com.javaee.common.utils;

import com.javaee.common.constant.CommonConstant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author qxk
 * @description: 日期时间工具（格式化/转换）
 */
public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.DATE_FORMAT);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.TIME_FORMAT);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.DATE_TIME_FORMAT);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.TIMESTAMP_FORMAT);

    /**
     * 获取当前日期
     * @return 当前日期
     */
    public static LocalDate nowDate() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间
     * @return 当前时间
     */
    public static LocalTime nowTime() {
        return LocalTime.now();
    }

    /**
     * 获取当前日期时间
     * @return 当前日期时间
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前时间戳
     * @return 当前时间戳
     */
    public static long nowTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 格式化日期
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * 格式化时间
     * @param time 时间
     * @return 格式化后的字符串
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return time.format(TIME_FORMATTER);
    }

    /**
     * 格式化日期时间
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 格式化日期时间（带毫秒）
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIMESTAMP_FORMATTER);
    }

    /**
     * 格式化日期
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(CommonConstant.DATE_FORMAT);
        return sdf.format(date);
    }

    /**
     * 格式化日期时间
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(CommonConstant.DATE_TIME_FORMAT);
        return sdf.format(date);
    }

    /**
     * 解析日期
     * @param dateStr 日期字符串
     * @return 日期
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * 解析日期时间
     * @param dateTimeStr 日期时间字符串
     * @return 日期时间
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
    }

    /**
     * 解析日期
     * @param dateStr 日期字符串
     * @return 日期
     */
    public static Date parseDateToDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(CommonConstant.DATE_FORMAT);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 解析日期时间
     * @param dateTimeStr 日期时间字符串
     * @return 日期
     */
    public static Date parseDateTimeToDate(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(CommonConstant.DATE_TIME_FORMAT);
            return sdf.parse(dateTimeStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * LocalDate转Date
     * @param localDate LocalDate
     * @return Date
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDateTime转Date
     * @param localDateTime LocalDateTime
     * @return Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Date转LocalDate
     * @param date Date
     * @return LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Date转LocalDateTime
     * @param date Date
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 时间戳转LocalDateTime
     * @param timestamp 时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime timestampToDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime转时间戳
     * @param dateTime 日期时间
     * @return 时间戳
     */
    public static long dateTimeToTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 计算两个日期之间的天数差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数差
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays();
    }

    /**
     * 计算两个日期时间之间的小时差
     * @param startDateTime 开始日期时间
     * @param endDateTime 结束日期时间
     * @return 小时差
     */
    public static long hoursBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return Duration.between(startDateTime, endDateTime).toHours();
    }

    /**
     * 计算两个日期时间之间的分钟差
     * @param startDateTime 开始日期时间
     * @param endDateTime 结束日期时间
     * @return 分钟差
     */
    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return Duration.between(startDateTime, endDateTime).toMinutes();
    }

    /**
     * 计算两个日期时间之间的秒差
     * @param startDateTime 开始日期时间
     * @param endDateTime 结束日期时间
     * @return 秒差
     */
    public static long secondsBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return Duration.between(startDateTime, endDateTime).getSeconds();
    }

    /**
     * 日期加天数
     * @param date 日期
     * @param days 天数
     * @return 新日期
     */
    public static LocalDate plusDays(LocalDate date, long days) {
        if (date == null) {
            return null;
        }
        return date.plusDays(days);
    }

    /**
     * 日期时间加天数
     * @param dateTime 日期时间
     * @param days 天数
     * @return 新日期时间
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }

    /**
     * 日期时间加小时
     * @param dateTime 日期时间
     * @param hours 小时数
     * @return 新日期时间
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }

    /**
     * 日期时间加分钟
     * @param dateTime 日期时间
     * @param minutes 分钟数
     * @return 新日期时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMinutes(minutes);
    }

    /**
     * 判断是否为同一天
     * @param dateTime1 日期时间1
     * @param dateTime2 日期时间2
     * @return 是否为同一天
     */
    public static boolean isSameDay(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.toLocalDate().equals(dateTime2.toLocalDate());
    }

    /**
     * 判断是否为今天
     * @param dateTime 日期时间
     * @return 是否为今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * 获取当天的开始时间
     * @param date 日期
     * @return 开始时间
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * 获取当天的结束时间
     * @param date 日期
     * @return 结束时间
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.MAX);
    }
}
