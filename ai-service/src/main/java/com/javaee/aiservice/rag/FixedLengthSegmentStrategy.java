package com.javaee.aiservice.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedLengthSegmentStrategy implements SegmentStrategy {

    private static final Logger log = LoggerFactory.getLogger(FixedLengthSegmentStrategy.class);

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP = 100;

    private static final String CHINESE_PUNCTUATION = "。！？；：、，";

    private final int chunkSize;
    private final int overlap;

    public FixedLengthSegmentStrategy() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public FixedLengthSegmentStrategy(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    public List<Segment> segment(String documentId, String content) {
        log.info("执行固定长度分段: documentId={}, chunkSize={}, overlap={}", documentId, chunkSize, overlap);

        List<Segment> segments = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            log.warn("文档内容为空，跳过分段");
            return segments;
        }

        int totalLength = content.length();
        int index = 0;
        int start = 0;

        while (start < totalLength) {
            int end = Math.min(start + chunkSize, totalLength);

            if (end < totalLength) {
                int breakPoint = findBreakPoint(content, start, end);

                if (breakPoint >= start) {
                    end = breakPoint + 1;
                }
            }

            String chunkContent = content.substring(start, end).trim();

            if (!chunkContent.isEmpty()) {
                Segment segment = new Segment(documentId, chunkContent, index);
                segments.add(segment);
                log.debug("生成分段: segmentId={}, charCount={}", segment.getSegmentId(), chunkContent.length());
                index++;
            }

            if (end >= totalLength) {
                break;
            }

            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        log.info("固定长度分段完成: documentId={}, 生成{}个分段", documentId, segments.size());
        return segments;
    }

    private int findBreakPoint(String content, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = content.charAt(i);
            if (CHINESE_PUNCTUATION.indexOf(c) >= 0) {
                return i;
            }
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getStrategyName() {
        return "FIXED_LENGTH";
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getOverlap() {
        return overlap;
    }
}