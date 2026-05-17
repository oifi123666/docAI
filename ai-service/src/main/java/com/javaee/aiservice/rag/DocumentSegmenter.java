package com.javaee.aiservice.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DocumentSegmenter {

    private static final Logger log = LoggerFactory.getLogger(DocumentSegmenter.class);

    public enum StrategyType {
        AUTO("自动选择"),
        FIXED_LENGTH("固定长度分段"),
        CHAPTER("按章节分段"),
        SEMANTIC("语义分段"),
        HYBRID("混合分段");

        private final String description;

        StrategyType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Autowired
    private FixedLengthSegmentStrategy fixedLengthStrategy;

    @Autowired
    private ChapterSegmentStrategy chapterStrategy;

    @Autowired
    private SemanticSegmentStrategy semanticStrategy;

    private StrategyType defaultStrategy = StrategyType.CHAPTER;

    public List<SegmentStrategy.Segment> segment(String documentId, String content) {
        return segment(documentId, content, defaultStrategy);
    }

    public List<SegmentStrategy.Segment> segment(String documentId, String content, StrategyType strategyType) {
        log.info("开始文档分段: documentId={}, strategy={}", documentId, strategyType);

        if (strategyType == null) {
            strategyType = defaultStrategy;
        }
        if (strategyType == StrategyType.AUTO) {
            return segmentByAuto(documentId, content);
        }
        if (strategyType == StrategyType.HYBRID) {
            return segmentWithHybrid(documentId, content);
        }

        SegmentStrategy strategy = getSingleStrategy(strategyType);
        List<SegmentStrategy.Segment> segments = strategy.segment(documentId, content);

        log.info("文档分段完成: documentId={}, strategy={}, segmentCount={}",
                documentId, strategyType, segments.size());

        return segments;
    }

    public List<SegmentStrategy.Segment> segmentWithHybrid(String documentId, String content) {
        log.info("执行混合分段: documentId={}", documentId);

        List<SegmentStrategy.Segment> chapterSegments = chapterStrategy.segment(documentId, content);
        List<SegmentStrategy.Segment> semanticSegments = new ArrayList<>();

        for (SegmentStrategy.Segment chapter : chapterSegments) {
            List<SegmentStrategy.Segment> chapterSemanticSegments =
                    semanticStrategy.segment(documentId + "_hybrid_chapter_" + chapter.getIndex(), chapter.getContent());
            if (chapterSemanticSegments.isEmpty()) {
                semanticSegments.add(chapter);
                continue;
            }
            for (SegmentStrategy.Segment semantic : chapterSemanticSegments) {
                semantic.setTitle(firstNonBlank(chapter.getTitle(), semantic.getTitle()));
                semanticSegments.add(semantic);
            }
        }

        List<SegmentStrategy.Segment> finalResult = new ArrayList<>();
        Set<String> contentSeen = new LinkedHashSet<>();
        for (SegmentStrategy.Segment seg : semanticSegments) {
            String normalized = seg.getContent().toLowerCase().replaceAll("\\s+", "");
            if (contentSeen.add(normalized)) {
                finalResult.add(seg);
            }
        }

        for (int i = 0; i < finalResult.size(); i++) {
            SegmentStrategy.Segment segment = finalResult.get(i);
            segment.setSegmentId(documentId + "_hybrid_" + i);
            segment.setDocumentId(documentId);
            segment.setIndex(i);
        }

        log.info("混合分段完成: documentId={}, 最终分段数={}", documentId, finalResult.size());
        return finalResult;
    }

    public List<SegmentStrategy.Segment> segmentByAuto(String documentId, String content) {
        log.info("执行自动分段策略选择: documentId={}", documentId);

        if (content == null || content.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        boolean hasChapterMarkers = detectChapterMarkers(content);
        boolean isLongDocument = content.length() > 2000;

        StrategyType selectedStrategy;
        if (hasChapterMarkers) {
            selectedStrategy = StrategyType.CHAPTER;
            log.info("检测到章节结构，使用按章节分段");
        } else if (isLongDocument) {
            selectedStrategy = StrategyType.SEMANTIC;
            log.info("长文档且无明显章节结构，使用语义分段");
        } else {
            selectedStrategy = StrategyType.FIXED_LENGTH;
            log.info("短文档，使用固定长度分段");
        }

        return segment(documentId, content, selectedStrategy);
    }

    private boolean detectChapterMarkers(String content) {
        String[] chapterPatterns = {
            "第1章", "第2章", "第1节", "第2节",
            "第1篇", "第2篇", "第1部", "第2部",
            "Chapter", "Section", "第[一二三四五六七八九十]"
        };

        for (String pattern : chapterPatterns) {
            if (content.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private SegmentStrategy getSingleStrategy(StrategyType strategyType) {
        switch (strategyType) {
            case FIXED_LENGTH:
                return fixedLengthStrategy;
            case CHAPTER:
                return chapterStrategy;
            case SEMANTIC:
                return semanticStrategy;
            default:
                return chapterStrategy;
        }
    }

    public void setDefaultStrategy(StrategyType strategyType) {
        this.defaultStrategy = strategyType;
    }

    public StrategyType getDefaultStrategy() {
        return defaultStrategy;
    }

    public Map<String, String> getAvailableStrategies() {
        Map<String, String> strategies = new HashMap<>();
        for (StrategyType type : StrategyType.values()) {
            strategies.put(type.name(), type.getDescription());
        }
        return strategies;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : second;
    }
}
