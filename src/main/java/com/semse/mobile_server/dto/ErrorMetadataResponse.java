package com.semse.mobile_server.dto;

import com.semse.mobile_server.entity.ErrorMetadata;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorMetadataResponse {

    private String errorCode;
    private String category;
    private String errorName;
    private String severity;
    private String description;
    private String solution;

    public static ErrorMetadataResponse from(ErrorMetadata e) {
        return ErrorMetadataResponse.builder()
                .errorCode(e.getErrorCode())
                .category(e.getCategory())
                .errorName(e.getErrorName())
                .severity(e.getSeverity().name())
                .description(e.getDescription())
                .solution(e.getSolution())
                .build();
    }
}