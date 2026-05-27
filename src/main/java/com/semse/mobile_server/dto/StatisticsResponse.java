package com.semse.mobile_server.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 요약 통계 응답 DTO 입니다.
 *
 * <p>AdminPC-Server 의 {@code GET /api/dashboard/summary} 응답을 기반으로 구성됩니다.</p>
 *
 * <ul>
 *   <li>{@code lockedDevices} - CRITICAL 오류로 잠금된 장비 수
 *       (AdminPC-Server 는 LOCKED 상태를 {@code device_status[id]["status"] = "LOCKED"}로 관리)</li>
 * </ul>
 */
@Getter
@Builder
public class StatisticsResponse {
    private int totalDevices;
    private int runningDevices;
    private int lockedDevices;   // CRITICAL 오류로 잠금된 장비 수
    private int errorDevices;
    private int totalInspections;
    private int okCount;
    private int ngCount;
    private double ngRate;
    private int errorCount;
}