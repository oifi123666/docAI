package com.javaee.documentservice.dto;

import lombok.Data;

@Data
public class DocumentCompareDTO {
    private Integer originalVersion;
    private Integer revisedVersion;
}
