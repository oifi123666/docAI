package com.javaee.aiservice.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChapterSegmentStrategy implements SegmentStrategy {

    private static final Logger log = LoggerFactory.getLogger(ChapterSegmentStrategy.class);

    private static final int DEFAULT_MIN_CHAPTER_LENGTH = 100;
    private static final int DEFAULT_MAX_CHAPTER_LENGTH = 5000;

    private static final Pattern[] CHAPTER_PATTERNS = {
        Pattern.compile("^第[一二三四五六七八九十百千\\d]+[章节篇部]\\s*[:：]?\\s*(.+)$", Pattern.MULTILINE),
        Pattern.compile("^[0-9]+\\.[0-9]+\\s+[:：]?\\s*(.+)$", Pattern.MULTILINE),
        Pattern.compile("^Chapter\\s+[0-9]+\\s*[:：]?\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
        Pattern.compile("^Section\\s+[0-9]+\\s*[:：]?\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
        Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE),
        Pattern.compile("^\\[([^\"]+)\\]\\s*$", Pattern.MULTILINE),
    };

    private static final Pattern[] TITLE_CLEAN_PATTERNS = {
        Pattern.compile("^第[一二三四五六七八九十百千\\d]+[章节篇部]\\s*[:：]?\\s*"),
        Pattern.compile("^[0-9]+\\.[0-9]+\\s+[:：]?\\s*"),
        Pattern.compile("^Chapter\\s+[0-9]+\\s*[:：]?\\s*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Section\\s+[0-9]+\\s*[:：]?\\s*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^#+\\s+")
    };

    private final int minChapterLength;
    private final int maxChapterLength;

    public ChapterSegmentStrategy() {
        this(DEFAULT_MIN_CHAPTER_LENGTH, DEFAULT_MAX_CHAPTER_LENGTH);
    }

    public ChapterSegmentStrategy(int minChapterLength, int maxChapterLength) {
        this.minChapterLength = minChapterLength;
        this.maxChapterLength = maxChapterLength;
    }

    @Override
    public List<Segment> segment(String documentId, String content) {
        log.info("执行按章节分段: documentId={}, minChapterLength={}, maxChapterLength={}",
                documentId, minChapterLength, maxChapterLength);

        List<Segment> segments = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            log.warn("文档内容为空，跳过分段");
            return segments;
        }

        List<ChapterMatch> chapters = findChapters(content);

        if (chapters.isEmpty()) {
            log.info("未检测到章节结构，使用固定长度分段");
            FixedLengthSegmentStrategy fallback = new FixedLengthSegmentStrategy(maxChapterLength, 0);
            return fallback.segment(documentId, content);
        }

        for (int i = 0; i < chapters.size(); i++) {
            ChapterMatch chapter = chapters.get(i);
            int start = chapter.start;
            int end = (i < chapters.size() - 1) ? chapters.get(i + 1).start : content.length();

            String chapterContent = content.substring(start, end).trim();

            if (chapterContent.length() < minChapterLength) {
                log.debug("章节内容过短，跳过: {}", chapter.title);
                continue;
            }

            if (chapterContent.length() > maxChapterLength) {
                log.info("章节内容过长，进行子分段: {} (length={})", chapter.title, chapterContent.length());
                List<Segment> subSegments = splitLargeChapter(documentId, chapter, chapterContent, segments.size());
                segments.addAll(subSegments);
            } else {
                Segment segment = new Segment(
                        generateSegmentId(documentId, segments.size()),
                        documentId,
                        chapterContent,
                        segments.size(),
                        chapter.title
                );
                segments.add(segment);
                log.debug("生成分段: segmentId={}, title={}, charCount={}",
                        segment.getSegmentId(), chapter.title, chapterContent.length());
            }
        }

        log.info("按章节分段完成: documentId={}, 生成{}个分段", documentId, segments.size());
        return segments;
    }

    private List<ChapterMatch> findChapters(String content) {
        List<ChapterMatch> chapters = new ArrayList<>();

        for (Pattern pattern : CHAPTER_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String title = matcher.group(1) != null ? matcher.group(1) : matcher.group(0);
                title = cleanTitle(title);
                if (!title.isEmpty()) {
                    chapters.add(new ChapterMatch(matcher.start(), title));
                }
            }
            if (!chapters.isEmpty()) {
                break;
            }
        }

        chapters.sort((a, b) -> Integer.compare(a.start, b.start));
        return chapters;
    }

    private String cleanTitle(String title) {
        String cleaned = title;
        for (Pattern pattern : TITLE_CLEAN_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceFirst("");
        }
        return cleaned.trim();
    }

    private List<Segment> splitLargeChapter(String documentId, ChapterMatch chapter,
                                           String content, int startIndex) {
        List<Segment> subSegments = new ArrayList<>();
        FixedLengthSegmentStrategy splitter = new FixedLengthSegmentStrategy(maxChapterLength, 0);
        List<Segment> chunks = splitter.segment(documentId + "_ch_" + startIndex, content);

        for (int i = 0; i < chunks.size(); i++) {
            Segment chunk = chunks.get(i);
            String subTitle = chapter.title + " - 第" + (i + 1) + "部分";
            Segment segment = new Segment(
                    generateSegmentId(documentId, startIndex + i),
                    documentId,
                    chunk.getContent(),
                    startIndex + i,
                    subTitle
            );
            subSegments.add(segment);
        }

        return subSegments;
    }

    private String generateSegmentId(String documentId, int index) {
        return documentId + "_chapter_" + index;
    }

    @Override
    public String getStrategyName() {
        return "CHAPTER";
    }

    public int getMinChapterLength() {
        return minChapterLength;
    }

    public int getMaxChapterLength() {
        return maxChapterLength;
    }

    private static class ChapterMatch {
        int start;
        String title;

        ChapterMatch(int start, String title) {
            this.start = start;
            this.title = title;
        }
    }
}