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

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionLogRepository inspectionLogRepository;

    public void saveInspectionData(JsonObject json) {

        JsonObject header = json.getAsJsonObject("header");
        JsonObject body = json.getAsJsonObject("body");

        InspectionLog log = new InspectionLog();

        // 기본 정보
        log.setDeviceId(json.get("device_id").getAsString());
        log.setBatchId(json.get("batch_id").getAsString());
        log.setModelName(json.get("model_name").getAsString());

        log.setSequence(json.get("sequence").getAsInt());
        log.setMachineStatus(MachineStatus.valueOf(json.get("machine_status").getAsString()));

        // sensor_data
        JsonObject sensor = json.getAsJsonObject("sensor_data");

        log.setTemperature(sensor.get("temperature").getAsDouble());
        log.setVibrationX(sensor.get("vibration_x").getAsDouble());
        log.setVibrationY(sensor.get("vibration_y").getAsDouble());
        log.setIllumination(sensor.get("illumination").getAsDouble());

        if (sensor.has("humidity") && !sensor.get("humidity").isJsonNull()) {
            log.setHumidity(sensor.get("humidity").getAsDouble());
        }

        // timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.setTimestamp(LocalDateTime.parse(json.get("timestamp").getAsString(), formatter));
        log.setReceivedAt(LocalDateTime.now());

        // vision_result
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

        // status_info
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