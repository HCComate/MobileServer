package com.semse.mobile_server.dto;

public record StatusInfoResponse(
        String code,
        String msg,
        String severity,
        String direction,
        String partLocation,
        Boolean isCaptureRequired
) {
}