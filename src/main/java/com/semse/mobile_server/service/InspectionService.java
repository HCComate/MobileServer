package com.semse.mobile_server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.semse.mobile_server.entity.*;
import com.semse.mobile_server.repository.InspectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionLogRepository inspectionLogRepository;

    public void saveInspectionData(JsonObject json) {

        JsonObject header = json.getAsJsonObject("header");
        JsonObject body = json.getAsJsonObject("body");

        InspectionLog log = new InspectionLog();

        // header
        log.setDeviceId(header.get("device_id").getAsString());
        log.setBatchId(header.get("batch_id").getAsString());
        log.setModelName(header.get("model_name").getAsString());

        // body
        log.setSequence(body.get("sequence").getAsInt());
        log.setMachineStatus(MachineStatus.valueOf(body.get("machine_status").getAsString()));

        // sensor data
        JsonObject sensor = body.getAsJsonObject("sensor_data");
        log.setTemperature(sensor.get("temperature").getAsDouble());
        log.setVibrationX(sensor.get("vibration_x").getAsDouble());
        log.setVibrationY(sensor.get("vibration_y").getAsDouble());
        log.setIllumination(sensor.get("illumination").getAsDouble());
        log.setHumidity(sensor.get("humidity").getAsDouble());

        log.setTimestamp(LocalDateTime.parse(body.get("timestamp").getAsString()));
        log.setReceivedAt(LocalDateTime.now());

        // status_info
        JsonArray statusArray = body.getAsJsonArray("status_info");
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

        // vision_result
        JsonObject vision = body.getAsJsonObject("vision_result");

        VisionResult vr = new VisionResult();
        vr.setResult(vision.get("result").getAsString());
        vr.setDefectType(vision.get("defect_type").getAsString());
        vr.setConfidence(vision.get("confidence").getAsDouble());
        vr.setInspectionArea(vision.get("inspection_area").getAsString());
        vr.setImageUrl(vision.get("image_url").getAsString());

        vr.setInspectionLog(log);
        log.setVisionResult(vr);

        // 저장
        inspectionLogRepository.save(log);
    }
}