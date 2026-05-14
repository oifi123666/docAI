package com.javaee.aiservice.config;

import com.javaee.aiservice.rag.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SegmentConfig {

    @Bean
    public DocumentSegmenter.StrategyType defaultSegmentStrategy() {
        return DocumentSegmenter.StrategyType.CHAPTER;
    }

    @Bean
    public FixedLengthSegmentStrategy fixedLengthSegmentStrategy() {
        return new FixedLengthSegmentStrategy(500, 50);
    }

    @Bean
    public ChapterSegmentStrategy chapterSegmentStrategy() {
        return new ChapterSegmentStrategy(100, 5000);
    }

    @Bean
    public SemanticSegmentStrategy semanticSegmentStrategy() {
        return new SemanticSegmentStrategy(300, 100, 75);
    }
}