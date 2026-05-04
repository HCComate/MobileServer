package com.semse.mobile_server.service;

import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.repository.InspectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.semse.mobile_server.dto.DeviceDetailResponse;
import com.semse.mobile_server.dto.StatusInfoResponse;
import com.semse.mobile_server.dto.VisionResultResponse;
import com.semse.mobile_server.dto.DeviceListResponse;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final InspectionLogRepository inspectionLogRepository;

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
                .map(log -> new DeviceListResponse(
                        log.getDeviceId(),
                        log.getMachineStatus().name(),
                        log.getModelName(),
                        log.getTimestamp()
                ))
                .toList();
    }
    public DeviceDetailResponse getDeviceDetail(String deviceId) {
        InspectionLog log = inspectionLogRepository
                .findTopByDeviceIdOrderByTimestampDesc(deviceId)
                .orElse(null);

        if (log == null) {
            return null;
        }

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
                log.getTemperature(),
                log.getVibrationX(),
                log.getVibrationY(),
                log.getIllumination(),
                log.getHumidity(),
                log.getTimestamp(),
                statusInfos,
                visionResult
        );
    }
}