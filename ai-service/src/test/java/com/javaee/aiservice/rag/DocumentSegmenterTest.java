package com.javaee.aiservice.rag;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentSegmenterTest {

    @Test
    void hybridStrategySegmentsSemanticallyWithinEachChapter() {
        DocumentSegmenter segmenter = new DocumentSegmenter();
        ReflectionTestUtils.setField(segmenter, "chapterStrategy",
                new StubChapterSegmentStrategy(List.of(
                        segment("doc-1", "chapter one content", 0, "第一章"),
                        segment("doc-1", "chapter two content", 1, "第二章"))));
        ReflectionTestUtils.setField(segmenter, "semanticStrategy",
                new StubSemanticSegmentStrategy());

        List<SegmentStrategy.Segment> segments = segmenter.segment(
                "doc-1",
                "第1章 标题\nchapter one content\n第2章 标题\nchapter two content",
                DocumentSegmenter.StrategyType.HYBRID);

        assertThat(segments).hasSize(2);
        assertThat(segments).extracting(SegmentStrategy.Segment::getSegmentId)
                .containsExactly("doc-1_hybrid_0", "doc-1_hybrid_1");
        assertThat(segments).extracting(SegmentStrategy.Segment::getContent)
                .containsExactly("chapter one content semantic", "chapter two content semantic");
        assertThat(segments).extracting(SegmentStrategy.Segment::getTitle).containsExactly("第一章", "第二章");
        assertThat(segments).extracting(SegmentStrategy.Segment::getIndex).containsExactly(0, 1);
    }

    @Test
    void autoStrategyUsesAutomaticSelection() {
        DocumentSegmenter segmenter = new DocumentSegmenter();
        ReflectionTestUtils.setField(segmenter, "fixedLengthStrategy",
                new StubFixedLengthSegmentStrategy(List.of(segment("doc-1", "fixed content", 0))));

        List<SegmentStrategy.Segment> segments = segmenter.segment(
                "doc-1",
                "短文档内容",
                DocumentSegmenter.StrategyType.AUTO);

        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).getContent()).isEqualTo("fixed content");
    }

    private static SegmentStrategy.Segment segment(String documentId, String content, int index) {
        return new SegmentStrategy.Segment(documentId, content, index);
    }

    private static SegmentStrategy.Segment segment(String documentId, String content, int index, String title) {
        return new SegmentStrategy.Segment(documentId + "_chapter_" + index, documentId, content, index, title);
    }

    private static class StubFixedLengthSegmentStrategy extends FixedLengthSegmentStrategy {
        private final List<Segment> segments;

        private StubFixedLengthSegmentStrategy(List<Segment> segments) {
            this.segments = segments;
        }

        @Override
        public List<Segment> segment(String documentId, String content) {
            return segments;
        }
    }

    private static class StubChapterSegmentStrategy extends ChapterSegmentStrategy {
        private final List<Segment> segments;

        private StubChapterSegmentStrategy(List<Segment> segments) {
            this.segments = segments;
        }

        @Override
        public List<Segment> segment(String documentId, String content) {
            return segments;
        }
    }

    private static class StubSemanticSegmentStrategy extends SemanticSegmentStrategy {
        @Override
        public List<Segment> segment(String documentId, String content) {
            return List.of(new Segment(documentId, content + " semantic", 0));
        }
    }
}
