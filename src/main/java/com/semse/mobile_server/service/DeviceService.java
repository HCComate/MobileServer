package com.semse.mobile_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.DeviceListResponse;
import com.semse.mobile_server.dto.StatusInfoResponse;
import com.semse.mobile_server.dto.VisionResultResponse;
import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.entity.MachineStatus;
import com.semse.mobile_server.entity.Severity;
import com.semse.mobile_server.repository.InspectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    @Value("${admin.pc.username}")
    private String adminUsername;

    @Value("${admin.pc.password}")
    private String adminPassword;

    /** AdminPC-Server 인증 토큰 (지연 초기화) */
    private String token;

    // ──────────────────────────────────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 전체 장비의 최신 상태 목록을 반환합니다.
     *
     * <p>장비별 가장 최근 InspectionLog 를 기준으로 요약 정보를 구성하며,
     * powerStatus 는 machineStatus 로부터 추론합니다.</p>
     */
    public List<DeviceListResponse> getAllDevices() {
        List<InspectionLog> allLogs = inspectionLogRepository.findAll();

        Map<String, InspectionLog> latestMap = new HashMap<>();
        for (InspectionLog log : allLogs) {
            String deviceId = log.getDeviceId();
            if (!latestMap.containsKey(deviceId) ||
                    log.getTimestamp().isAfter(latestMap.get(deviceId).getTimestamp())) {
                latestMap.put(deviceId, log);
            }
        }

        return latestMap.values().stream()
                .map(log -> {
                    String visionResult = (log.getVisionResult() != null)
                            ? log.getVisionResult().getResult()
                            : null;

                    // statusInfos 중 가장 높은 severity
                    String severity = log.getStatusInfos().stream()
                            .filter(s -> s.getSeverity() != null)
                            .map(s -> s.getSeverity().ordinal())
                            .max(Integer::compareTo)
                            .map(i -> Severity.values()[i].name())
                            .orElse(null);

                    String machineStatus = log.getMachineStatus().name();

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
                .toList();
    }

    /**
     * 특정 장비의 최신 상세 정보를 반환합니다.
     */
    public DeviceDetailResponse getDeviceDetail(String deviceId) {
        InspectionLog log = inspectionLogRepository
                .findTopByDeviceIdOrderByTimestampDesc(deviceId)
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
            if (token == null) {
                loginToAdminPc();
            }
            callResolveApi(deviceId);
            System.out.println("[DeviceService] AdminPC-Server 오류 해제 성공 - deviceId: " + deviceId);
        } catch (Exception e) {
            // 토큰 만료 가능성 → 재로그인 후 1회 재시도
            System.out.println("[DeviceService] 오류 해제 실패, 재로그인 후 재시도: " + e.getMessage());
            token = null;
            try {
                loginToAdminPc();
                callResolveApi(deviceId);
                System.out.println("[DeviceService] 재시도 후 오류 해제 성공 - deviceId: " + deviceId);
            } catch (Exception retryEx) {
                throw new RuntimeException("AdminPC-Server 오류 해제 실패: " + retryEx.getMessage(), retryEx);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────────



    /**
     * AdminPC-Server 에 로그인하여 토큰을 갱신합니다.
     */
    private void loginToAdminPc() {
        String url = adminBaseUrl + "/api/auth/login";
        JsonObject body = new JsonObject();
        body.addProperty("username", adminUsername);
        body.addProperty("password", adminPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
        this.token = json.get("token").getAsString();
        System.out.println("[DeviceService] AdminPC-Server 로그인 성공");
    }

    private void callResolveApi(String deviceId) {
        String url = adminBaseUrl + "/api/devices/" + deviceId + "/resolve";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
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
                log.getMachineStatus().name(),
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
