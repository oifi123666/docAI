package com.javaee.aiservice.aiops.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TimerStats {

    private final int count;
    private final long min;
    private final long max;
    private final double avg;
    private final long p95;
    private final long p99;

    public TimerStats(int count, long min, long max, double avg, long p95, long p99) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.avg = avg;
        this.p95 = p95;
        this.p99 = p99;
    }

    public int getCount() {
        return count;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public double getAvg() {
        return avg;
    }

    public long getP95() {
        return p95;
    }

    public long getP99() {
        return p99;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("min", min);
        result.put("max", max);
        result.put("avg", avg);
        result.put("p95", p95);
        result.put("p99", p99);
        return result;
    }

    public static TimerStats empty() {
        return new TimerStats(0, 0L, 0L, 0.0, 0L, 0L);
    }
}
