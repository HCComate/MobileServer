package com.semse.mobile_server.dto;

import java.time.LocalDateTime;

/**
 * 장비 목록 조회 시 반환되는 요약 응답 DTO 입니다.
 *
 * <p>{@code machineStatus} 는 {@link com.semse.mobile_server.entity.MachineStatus} 의 name() 값입니다.</p>
 */
public record DeviceListResponse(
        String deviceId,
        String machineStatus,
        String modelName,
        LocalDateTime timestamp,
        String visionResult,      // OK / NG
        String severity,          // LOW / MEDIUM / HIGH / CRITICAL
        Integer lastSequence
) {
}
