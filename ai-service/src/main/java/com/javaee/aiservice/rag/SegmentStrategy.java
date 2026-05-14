package com.javaee.aiservice.rag;

import java.util.List;

public interface SegmentStrategy {

    List<Segment> segment(String documentId, String content);

    String getStrategyName();

    static class Segment {
        private String segmentId;
        private String documentId;
        private String content;
        private int index;
        private String title;
        private int charCount;

        public Segment(String documentId, String content, int index) {
            this.segmentId = generateSegmentId(documentId, index);
            this.documentId = documentId;
            this.content = content;
            this.index = index;
            this.charCount = content != null ? content.length() : 0;
        }

        public Segment(String segmentId, String documentId, String content, int index, String title) {
            this.segmentId = segmentId;
            this.documentId = documentId;
            this.content = content;
            this.index = index;
            this.title = title;
            this.charCount = content != null ? content.length() : 0;
        }

        private String generateSegmentId(String documentId, int index) {
            return documentId + "_seg_" + index;
        }

        public String getSegmentId() { return segmentId; }
        public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getCharCount() { return charCount; }
        public void setCharCount(int charCount) { this.charCount = charCount; }
    }
}