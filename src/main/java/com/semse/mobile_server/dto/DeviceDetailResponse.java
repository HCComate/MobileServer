package com.semse.mobile_server.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeviceDetailResponse(
        String deviceId,
        String batchId,
        String modelName,
        Integer sequence,
        String machineStatus,
        Double temperature,
        Double vibrationX,
        Double vibrationY,
        Double illumination,
        Double humidity,
        LocalDateTime timestamp,
        List<StatusInfoResponse> statusInfos,
        VisionResultResponse visionResult
) {
}