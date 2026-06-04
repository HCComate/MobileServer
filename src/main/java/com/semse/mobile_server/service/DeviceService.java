package com.semse.mobile_server.service;

import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.DeviceListResponse;
import com.semse.mobile_server.dto.StatusInfoResponse;
import com.semse.mobile_server.dto.VisionResultResponse;
import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.entity.MachineStatus;
import com.semse.mobile_server.entity.Severity;
import com.semse.mobile_server.repository.InspectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 장비 관련 비즈니스 로직을 처리하는 서비스입니다.
 *
 * <p>주요 책임:</p>
 * <ul>
 *   <li>장비 목록 / 상세 조회 (최신 InspectionLog 기반)</li>
 *   <li>machineStatus 를 기반으로 powerStatus 추론 (STOP → OFF, 그 외 → ON)</li>
 *   <li>장비 오류 해제 요청을 AdminPC-Server 로 포워딩</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final InspectionLogRepository inspectionLogRepository;
    private final AdminPcAuthClient adminPcAuthClient;
    private final RestTemplate restTemplate; // RestTemplateConfig 빈 주입 (타임아웃 포함)

    // ──────────────────────────────────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 전체 장비의 최신 상태 목록을 반환합니다.
     *
     * <p>장비별 가장 최근 InspectionLog 를 기준으로 요약 정보를 구성하며,
     * powerStatus 는 machineStatus 로부터 추론합니다.</p>
     */
    // AdminPC-Server와 동일한 기본 IDLE 판정 시간 (초)
    private static final long IDLE_THRESHOLD_SECONDS = 10;

    /**
     * 마지막 로그가 RUN/STANDBY인데 일정 시간 새 로그가 없으면 IDLE로 보정한다.
     * STANDBY/IDLE/STOP/LOCKED는 AdminPC가 메모리에서 관리하는 실시간 상태라 로그에
     * 저장되지 않으므로, "검사 데이터가 끊긴" 장비를 가동 중으로 오인하지 않도록
     * 목록·상세 양쪽에서 동일하게 적용한다.
     */
    private String applyIdleAging(String machineStatus, java.time.LocalDateTime timestamp) {
        if (timestamp != null
                && ("RUN".equals(machineStatus) || "STANDBY".equals(machineStatus))
                && java.time.Duration.between(timestamp, java.time.LocalDateTime.now())
                        .getSeconds() > IDLE_THRESHOLD_SECONDS) {
            return "IDLE";
        }
        return machineStatus;
    }

    @Transactional(readOnly = true)
    public List<DeviceListResponse> getAllDevices() {
        List<InspectionLog> latestLogs = inspectionLogRepository.findLatestPerDevice();

        return latestLogs.stream()
                .map(log -> {
                    // RESOLVED(오류 수정 완료) 등 vision_result가 비어 있는 로그는
                    // result가 ""로 들어와 모니터링 비전결과가 빈칸이 된다. 빈값/null이면 "OK"로 보정.
                    String rawVision = (log.getVisionResult() != null)
                            ? log.getVisionResult().getResult()
                            : null;
                    String visionResult = (rawVision != null && !rawVision.isEmpty())
                            ? rawVision
                            : "OK";

                    String severity = log.getStatusInfos().stream()
                            .filter(s -> s.getSeverity() != null)
                            .map(s -> s.getSeverity().ordinal())
                            .max(Integer::compareTo)
                            .map(i -> Severity.values()[i].name())
                            .orElse(null);

                    String machineStatus = applyIdleAging(log.getMachineStatus().name(), log.getTimestamp());

                    return new DeviceListResponse(
                            log.getDeviceId(),
                            machineStatus,
                            log.getModelName(),
                            log.getTimestamp(),
                            visionResult,
                            severity,
                            log.getSequence()
                    );
                })
                .sorted(java.util.Comparator.comparing(DeviceListResponse::deviceId))
                .toList();
    }

    /**
     * 특정 장비의 최신 상세 정보를 반환합니다.
     */
    @Transactional(readOnly = true)
    public DeviceDetailResponse getDeviceDetail(String deviceId) {
        InspectionLog log = inspectionLogRepository
                .findTopByDeviceIdOrderByIdDesc(deviceId)
                .orElse(null);

        if (log == null) {
            return null;
        }

        return toDetailResponse(log);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 오류 해제
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 장비 오류 해제 요청을 AdminPC-Server 로 포워딩합니다.
     *
     * <p>AdminPC-Server 의 POST /api/devices/{deviceId}/resolve 엔드포인트를 호출합니다.
     * 인증 토큰이 없거나 만료된 경우 자동으로 재로그인을 시도합니다.</p>
     *
     * @param deviceId 오류를 해제할 장비 ID
     * @throws RuntimeException AdminPC-Server 호출 실패 시
     */
    public void resolveDevice(String deviceId) {
        System.out.println("[DeviceService] 오류 해제 요청 수신 - deviceId: " + deviceId);
        try {
            callResolveApi(deviceId);
            System.out.println("[DeviceService] AdminPC-Server 오류 해제 성공 - deviceId: " + deviceId);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.UNAUTHORIZED) {
                System.out.println("[DeviceService] 토큰 만료, 재시도: " + deviceId);
                adminPcAuthClient.invalidateToken();
                try {
                    callResolveApi(deviceId);
                    System.out.println("[DeviceService] 재시도 후 오류 해제 성공 - deviceId: " + deviceId);
                } catch (Exception retryEx) {
                    throw new RuntimeException("AdminPC-Server 오류 해제 실패: " + retryEx.getMessage(), retryEx);
                }
            } else if (e.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND) {
                // 장치가 이미 잠금 해제 상태 → 정상 처리 (이미 해결된 것)
                System.out.println("[DeviceService] 장치 이미 해제 상태 (정상): " + deviceId);
            } else if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN) {
                // 권한 없음 → 에러로 전달
                throw new RuntimeException("오류 해제 권한이 없습니다: " + deviceId);
            } else {
                throw new RuntimeException("AdminPC-Server 오류 해제 실패: " + e.getMessage(), e);
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // AdminPC-Server 연결 불가 → 경고만 출력, 앱은 정상 처리
            System.out.println("[DeviceService] AdminPC-Server 연결 불가, 로컬만 해제: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────────



    private void callResolveApi(String deviceId) {
        String url = adminPcAuthClient.getBaseUrl() + "/api/devices/" + deviceId + "/resolve";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminPcAuthClient.getValidToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private DeviceDetailResponse toDetailResponse(InspectionLog log) {
        List<StatusInfoResponse> statusInfos = log.getStatusInfos().stream()
                .map(s -> new StatusInfoResponse(
                        s.getCode(),
                        s.getMsg(),
                        s.getSeverity().name(),
                        s.getDirection(),
                        s.getPartLocation(),
                        s.getIsCaptureRequired()
                ))
                .toList();

        VisionResultResponse visionResult = null;
        if (log.getVisionResult() != null) {
            visionResult = new VisionResultResponse(
                    log.getVisionResult().getResult(),
                    log.getVisionResult().getDefectType(),
                    log.getVisionResult().getConfidence(),
                    log.getVisionResult().getInspectionArea(),
                    log.getVisionResult().getImageUrl()
            );
        }

        return new DeviceDetailResponse(
                log.getDeviceId(),
                log.getBatchId(),
                log.getModelName(),
                log.getSequence(),
                applyIdleAging(log.getMachineStatus().name(), log.getTimestamp()),
                log.getTimestamp(),
                log.getTemperature(),
                log.getVibrationX(),
                log.getVibrationY(),
                log.getIllumination(),
                log.getHumidity(),
                statusInfos,
                visionResult
        );
    }
}
