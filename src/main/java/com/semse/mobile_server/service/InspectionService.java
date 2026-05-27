package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.semse.mobile_server.entity.*;
import com.semse.mobile_server.repository.InspectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.StatusInfoResponse;
import com.semse.mobile_server.dto.VisionResultResponse;

/**
 * 검사 서비스입니다.
 *
 * <p>AdminPC-Server 로부터 수신한 검사 로그를 저장하고 조회합니다.</p>
 *
 * <p>machine_status 변환 규칙:</p>
 * <ul>
 *   <li>AdminPC-Server 가 전송하는 machine_status 가 "ERROR" 이고
 *       status_info 중 severity 가 "CRITICAL" 인 항목이 있으면 → LOCKED 로 저장합니다.</li>
 *   <li>AdminPC-Server 의 "STANDBY" 상태는 → IDLE 로 변환하여 저장합니다.</li>
 *   <li>그 외 상태(RUN / IDLE / STOP / ERROR)는 그대로 저장합니다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionLogRepository inspectionLogRepository;

    /**
     * AdminPC-Server 에서 수신한 검사 데이터를 DB 에 저장합니다.
     *
     * <p>데이터는 AdminPC-Server 의 폴링(/api/logs/after) 또는 Socket.IO(mobile_data_feed)
     * 두 경로 모두에서 호출될 수 있습니다. 두 경로 모두 flat JSON 구조로 전달됩니다.</p>
     *
     * @param json AdminPC-Server 에서 수신한 검사 로그 JSON
     */
    public void saveInspectionData(JsonObject json) {

        InspectionLog log = new InspectionLog();

        // ── 기본 정보 ──────────────────────────────────────────────────────────
        log.setDeviceId(json.get("device_id").getAsString());
        log.setBatchId(json.get("batch_id").getAsString());
        log.setModelName(json.get("model_name").getAsString());
        log.setSequence(json.get("sequence").getAsInt());

        // ── machine_status 변환 ────────────────────────────────────────────────
        // AdminPC-Server 의 STANDBY → IDLE 로 변환
        // AdminPC-Server 의 ERROR + CRITICAL status_info → LOCKED 로 변환
        String rawStatus = json.get("machine_status").getAsString();
        MachineStatus machineStatus = convertMachineStatus(rawStatus, json);
        log.setMachineStatus(machineStatus);

        // ── sensor_data ────────────────────────────────────────────────────────
        JsonObject sensor = json.getAsJsonObject("sensor_data");
        log.setTemperature(sensor.get("temperature").getAsDouble());
        log.setVibrationX(sensor.get("vibration_x").getAsDouble());
        log.setVibrationY(sensor.get("vibration_y").getAsDouble());
        log.setIllumination(sensor.get("illumination").getAsDouble());
        if (sensor.has("humidity") && !sensor.get("humidity").isJsonNull()) {
            log.setHumidity(sensor.get("humidity").getAsDouble());
        }

        // ── timestamp ──────────────────────────────────────────────────────────
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.setTimestamp(LocalDateTime.parse(json.get("timestamp").getAsString(), formatter));
        log.setReceivedAt(LocalDateTime.now());

        // ── vision_result ──────────────────────────────────────────────────────
        JsonObject vision = json.getAsJsonObject("vision_result");
        VisionResult vr = new VisionResult();
        vr.setResult(vision.get("result").getAsString());
        vr.setDefectType(vision.get("defect_type").getAsString());
        vr.setConfidence(vision.get("confidence").getAsDouble());
        vr.setInspectionArea(vision.get("inspection_area").getAsString());
        if (vision.has("image_url") && !vision.get("image_url").isJsonNull()) {
            vr.setImageUrl(vision.get("image_url").getAsString());
        }
        vr.setInspectionLog(log);
        log.setVisionResult(vr);

        // ── status_info ────────────────────────────────────────────────────────
        JsonArray statusArray = json.getAsJsonArray("status_info");
        for (int i = 0; i < statusArray.size(); i++) {
            JsonObject s = statusArray.get(i).getAsJsonObject();
            StatusInfo status = new StatusInfo();
            status.setCode(s.get("code").getAsString());
            status.setMsg(s.get("msg").getAsString());
            status.setSeverity(Severity.valueOf(s.get("severity").getAsString()));
            status.setDirection(s.get("direction").getAsString());
            status.setPartLocation(s.get("part_location").getAsString());
            status.setIsCaptureRequired(s.get("is_capture_required").getAsBoolean());
            status.setInspectionLog(log);
            log.getStatusInfos().add(status);
        }

        inspectionLogRepository.save(log);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * AdminPC-Server 의 rawStatus 를 MobileServer 의 MachineStatus 로 변환합니다.
     *
     * <p>AdminPC-Server 의 형식을 최대한 유지하며, CRITICAL 오류 시에만 LOCKED 로 변환합니다.</p>
     *
     * <ul>
     *   <li>"ERROR" + CRITICAL status_info 존재 → LOCKED</li>
     *   <li>그 외 (RUN, STANDBY, IDLE, STOP, ERROR 등) → 원본 값 그대로 파싱</li>
     * </ul>
     */
    private MachineStatus convertMachineStatus(String rawStatus, JsonObject json) {
        if ("ERROR".equals(rawStatus)) {
            JsonArray statusArray = json.has("status_info") ? json.getAsJsonArray("status_info") : new JsonArray();
            boolean hasCritical = false;
            for (int i = 0; i < statusArray.size(); i++) {
                JsonObject s = statusArray.get(i).getAsJsonObject();
                if (s.has("severity") && "CRITICAL".equals(s.get("severity").getAsString())) {
                    hasCritical = true;
                    break;
                }
            }
            return hasCritical ? MachineStatus.LOCKED : MachineStatus.ERROR;
        }
        try {
            return MachineStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            // 정의되지 않은 상태인 경우 ERROR 로 처리하거나 로그 기록
            System.out.println("[InspectionService] 알 수 없는 상태 수신: " + rawStatus);
            return MachineStatus.ERROR;
        }
    }

    public List<DeviceDetailResponse> getRecentLogs() {
        return inspectionLogRepository.findTop20ByOrderByTimestampDesc()
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    public DeviceDetailResponse getLatestByDevice(String deviceId) {
        return inspectionLogRepository
                .findTopByDeviceIdOrderByTimestampDesc(deviceId)
                .map(this::toDetailResponse)
                .orElse(null);
    }

    // 공통 변환 메서드 추가
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