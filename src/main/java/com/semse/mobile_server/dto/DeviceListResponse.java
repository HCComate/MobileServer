package com.semse.mobile_server.dto;

import java.time.LocalDateTime;

public record DeviceListResponse(
        String deviceId,
        String machineStatus,
        String modelName,
        LocalDateTime timestamp,
        String visionResult,   // OK / NG
        String severity,       // LOW / MEDIUM / HIGH / CRITICAL
        Integer lastSequence
) {
}