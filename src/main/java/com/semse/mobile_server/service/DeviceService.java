package com.semse.mobile_server.service;

import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.repository.InspectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final InspectionLogRepository inspectionLogRepository;

    public List<InspectionLog> getAllDevices() {
        List<InspectionLog> allLogs = inspectionLogRepository.findAll();

        Map<String, InspectionLog> latestMap = new HashMap<>();

        for (InspectionLog log : allLogs) {
            String deviceId = log.getDeviceId();

            if (!latestMap.containsKey(deviceId) ||
                    log.getTimestamp().isAfter(latestMap.get(deviceId).getTimestamp())) {
                latestMap.put(deviceId, log);
            }
        }
        return new ArrayList<>(latestMap.values());
    }
    public InspectionLog getDeviceDetail(String deviceId) {
        return inspectionLogRepository
                .findTopByDeviceIdOrderByTimestampDesc(deviceId)
                .orElse(null);
    }
}