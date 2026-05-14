package com.javaee.documentservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCompareVO {
    private String summary;
    private List<ChangeDetail> changes;
    private Integer addedLines;
    private Integer removedLines;
    private Integer modifiedLines;
    private Integer originalVersion;
    private Integer revisedVersion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeDetail {
        private String type;
        private Integer lineNumber;
        private String originalText;
        private String revisedText;
        private String description;
    }
}
