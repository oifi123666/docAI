package com.javaee.aiservice.aiops;

import com.javaee.aiservice.aiops.model.TimerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 监控服务
 * 使用滑动窗口保存指标，避免 timers 无限增长。
 */
@Component
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final String METRIC_PREFIX = "metric:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.aiops.metrics.retention-ms:900000}")
    private long retentionMs;

    private final ConcurrentMap<String, ArrayDeque<MetricPoint>> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ArrayDeque<TimerPoint>> timers = new ConcurrentHashMap<>();

    public void incrementCounter(String name) {
        incrementCounter(name, 1L);
    }

    public void incrementCounter(String name, long delta) {
        long now = System.currentTimeMillis();
        ArrayDeque<MetricPoint> points = counters.computeIfAbsent(name, key -> new ArrayDeque<>());
        synchronized (points) {
            points.addLast(new MetricPoint(now, delta));
            prune(points, now);
        }
        log.debug("计数器增量: name={}, delta={}", name, delta);
    }

    public void recordTimer(String name, long durationMs) {
        long now = System.currentTimeMillis();
        ArrayDeque<TimerPoint> points = timers.computeIfAbsent(name, key -> new ArrayDeque<>());
        synchronized (points) {
            points.addLast(new TimerPoint(now, durationMs));
            prune(points, now);
        }
        log.debug("记录耗时: name={}, duration={}ms", name, durationMs);
    }

    public long getCounter(String name) {
        return getCounter(name, retentionMs);
    }

    public long getCounter(String name, long windowMs) {
        ArrayDeque<MetricPoint> points = counters.get(name);
        if (points == null || points.isEmpty()) {
            return 0L;
        }
        long since = System.currentTimeMillis() - windowMs;
        synchronized (points) {
            return points.stream()
                    .filter(point -> point.timestamp >= since)
                    .mapToLong(point -> point.value)
                    .sum();
        }
    }

    public Map<String, Object> getTimerStats(String name) {
        return getTimerStats(name, retentionMs);
    }

    public Map<String, Object> getTimerStats(String name, long windowMs) {
        return getTimerStatsObject(name, windowMs).toMap();
    }

    public TimerStats getTimerStatsObject(String name, long windowMs) {
        ArrayDeque<TimerPoint> points = timers.get(name);
        if (points == null || points.isEmpty()) {
            return TimerStats.empty();
        }

        long since = System.currentTimeMillis() - windowMs;
        List<Long> values;
        synchronized (points) {
            values = points.stream()
                    .filter(point -> point.timestamp >= since)
                    .map(point -> point.durationMs)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
        if (values == null || values.isEmpty()) {
            return TimerStats.empty();
        }

        long min = values.get(0);
        long max = values.get(values.size() - 1);
        double avg = values.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = percentile(values, 0.95);
        long p99 = percentile(values, 0.99);

        return new TimerStats(values.size(), min, max, avg, p95, p99);
    }

    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("windows", Map.of(
                "1m", getMetricsForWindow(Duration.ofMinutes(1).toMillis()),
                "5m", getMetricsForWindow(Duration.ofMinutes(5).toMillis()),
                "15m", getMetricsForWindow(Duration.ofMinutes(15).toMillis())
        ));
        metrics.put("retentionMs", retentionMs);
        return metrics;
    }

    public Map<String, Object> getMetricsForWindow(long windowMs) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        for (String name : counters.keySet()) {
            metrics.put(name + ".count", getCounter(name, windowMs));
        }
        for (String name : timers.keySet()) {
            metrics.put(name + ".stats", getTimerStats(name, windowMs));
        }
        long total = getCounter("ai.requests", windowMs);
        long errors = getCounter("ai.errors", windowMs);
        metrics.put("ai.errorRate", total > 0 ? (double) errors / total : 0.0);
        metrics.put("windowMs", windowMs);
        return metrics;
    }

    public void saveMetrics() {
        String key = METRIC_PREFIX + System.currentTimeMillis();
        redisTemplate.opsForHash().putAll(key, getAllMetrics());
        redisTemplate.expire(key, Duration.ofHours(24));
        log.debug("指标已保存: key={}", key);
    }

    public void resetMetrics() {
        counters.clear();
        timers.clear();
        log.info("监控指标已重置");
    }

    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("metrics", getAllMetrics());
        return health;
    }

    @Scheduled(fixedDelayString = "${ai.aiops.metrics.persist-interval-ms:60000}")
    public void scheduledPersistMetrics() {
        saveMetrics();
        pruneAll();
    }

    private long percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private void pruneAll() {
        long now = System.currentTimeMillis();
        counters.values().forEach(points -> {
            synchronized (points) {
                prune(points, now);
            }
        });
        timers.values().forEach(points -> {
            synchronized (points) {
                prune(points, now);
            }
        });
    }

    private <T extends TimedPoint> void prune(ArrayDeque<T> points, long now) {
        long expireBefore = now - retentionMs;
        while (!points.isEmpty() && points.peekFirst().timestamp < expireBefore) {
            points.removeFirst();
        }
    }

    private abstract static class TimedPoint {
        final long timestamp;

        TimedPoint(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    private static final class MetricPoint extends TimedPoint {
        final long value;

        MetricPoint(long timestamp, long value) {
            super(timestamp);
            this.value = value;
        }
    }

    private static final class TimerPoint extends TimedPoint {
        final long durationMs;

        TimerPoint(long timestamp, long durationMs) {
            super(timestamp);
            this.durationMs = durationMs;
        }
    }
}
