package com.semse.mobile_server.dto;

public record VisionResultResponse(
        String result,
        String defectType,
        Double confidence,
        String inspectionArea,
        String imageUrl
) {
}