package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
 */
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionLogRepository inspectionLogRepository;

    /**
     * AdminPC-Server 에서 수신한 검사 데이터를 DB 에 저장합니다.
     * Null 안전성을 확보하도록 수정되었습니다.
     */
    public void saveInspectionData(JsonObject json) {
        if (json == null) return;

        InspectionLog log = new InspectionLog();

        // ── 기본 정보 ──────────────────────────────────────────────────────────
        log.setDeviceId(getStringSafe(json, "device_id"));
        log.setBatchId(getStringSafe(json, "batch_id"));
        log.setModelName(getStringSafe(json, "model_name"));
        log.setSequence(getIntSafe(json, "sequence"));

        // ── machine_status 변환 ────────────────────────────────────────────────
        String rawStatus = getStringSafe(json, "machine_status");
        MachineStatus machineStatus = convertMachineStatus(rawStatus, json);
        log.setMachineStatus(machineStatus);

        // ── sensor_data ────────────────────────────────────────────────────────
        if (json.has("sensor_data") && !json.get("sensor_data").isJsonNull()) {
            JsonObject sensor = json.getAsJsonObject("sensor_data");
            log.setTemperature(getDoubleSafe(sensor, "temperature"));
            log.setVibrationX(getDoubleSafe(sensor, "vibration_x"));
            log.setVibrationY(getDoubleSafe(sensor, "vibration_y"));
            log.setIllumination(getDoubleSafe(sensor, "illumination"));
            log.setHumidity(getDoubleSafe(sensor, "humidity"));
        }

        // ── timestamp ──────────────────────────────────────────────────────────
        String tsStr = getStringSafe(json, "timestamp");
        if (!tsStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                log.setTimestamp(LocalDateTime.parse(tsStr, formatter));
            } catch (Exception e) {
                log.setTimestamp(LocalDateTime.now());
            }
        } else {
            log.setTimestamp(LocalDateTime.now());
        }
        log.setReceivedAt(LocalDateTime.now());

        // ── vision_result ──────────────────────────────────────────────────────
        if (json.has("vision_result") && !json.get("vision_result").isJsonNull()) {
            JsonObject vision = json.getAsJsonObject("vision_result");
            VisionResult vr = new VisionResult();
            vr.setResult(getStringSafe(vision, "result"));
            vr.setDefectType(getStringSafe(vision, "defect_type"));
            vr.setConfidence(getDoubleSafe(vision, "confidence"));
            vr.setInspectionArea(getStringSafe(vision, "inspection_area"));
            vr.setImageUrl(getStringSafe(vision, "image_url"));
            vr.setInspectionLog(log);
            log.setVisionResult(vr);
        }

        // ── status_info ────────────────────────────────────────────────────────
        if (json.has("status_info") && json.get("status_info").isJsonArray()) {
            JsonArray statusArray = json.getAsJsonArray("status_info");
            for (int i = 0; i < statusArray.size(); i++) {
                JsonElement el = statusArray.get(i);
                if (el == null || !el.isJsonObject()) continue;
                
                JsonObject s = el.getAsJsonObject();
                StatusInfo status = new StatusInfo();
                status.setCode(getStringSafe(s, "code"));
                status.setMsg(getStringSafe(s, "msg"));
                
                String severityStr = getStringSafe(s, "severity");
                try {
                    status.setSeverity(Severity.valueOf(severityStr));
                } catch (Exception e) {
                    status.setSeverity(Severity.LOW);
                }
                
                status.setDirection(getStringSafe(s, "direction"));
                status.setPartLocation(getStringSafe(s, "part_location"));
                status.setIsCaptureRequired(getBooleanSafe(s, "is_capture_required"));
                status.setInspectionLog(log);
                log.getStatusInfos().add(status);
            }
        }

        inspectionLogRepository.save(log);
    }

    // ── JSON Helper Methods ──────────────────────────────────────────────────

    private String getStringSafe(JsonObject json, String key) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : "";
    }

    private double getDoubleSafe(JsonObject json, String key) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsDouble() : 0.0;
    }

    private int getIntSafe(JsonObject json, String key) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsInt() : 0;
    }

    private boolean getBooleanSafe(JsonObject json, String key) {
        return (json.has(key) && !json.get(key).isJsonNull()) && json.get(key).getAsBoolean();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────────

    private MachineStatus convertMachineStatus(String rawStatus, JsonObject json) {
        if ("ERROR".equals(rawStatus)) {
            JsonArray statusArray = (json.has("status_info") && json.get("status_info").isJsonArray()) 
                    ? json.getAsJsonArray("status_info") : new JsonArray();
            boolean hasCritical = false;
            for (int i = 0; i < statusArray.size(); i++) {
                JsonElement el = statusArray.get(i);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject s = el.getAsJsonObject();
                if (s.has("severity") && "CRITICAL".equals(getStringSafe(s, "severity"))) {
                    hasCritical = true;
                    break;
                }
            }
            return hasCritical ? MachineStatus.LOCKED : MachineStatus.ERROR;
        }
        try {
            return MachineStatus.valueOf(rawStatus);
        } catch (Exception e) {
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