package com.semse.mobile_server.service;

import com.semse.mobile_server.dto.StatisticsResponse;
import com.semse.mobile_server.repository.InspectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final InspectionLogRepository inspectionLogRepository;

    public StatisticsResponse getStatistics() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);

        var todayLogs = inspectionLogRepository.findByTimestampAfter(startOfDay);
        var last24hLogs = inspectionLogRepository.findByTimestampAfter(last24h);

        long okCount = todayLogs.stream()
                .filter(log -> log.getVisionResult() != null
                        && "OK".equals(log.getVisionResult().getResult()))
                .count();

        long ngCount = todayLogs.stream()
                .filter(log -> log.getVisionResult() != null
                        && "NG".equals(log.getVisionResult().getResult()))
                .count();

        Map<String, Long> ngByDevice = todayLogs.stream()
                .filter(log -> log.getVisionResult() != null
                        && "NG".equals(log.getVisionResult().getResult()))
                .collect(Collectors.groupingBy(
                        log -> log.getDeviceId(),
                        Collectors.counting()
                ));

        Map<String, Long> bySeverity = todayLogs.stream()
                .flatMap(log -> log.getStatusInfos().stream())
                .filter(s -> s.getSeverity() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getSeverity().name(),
                        Collectors.counting()
                ));

        return StatisticsResponse.builder()
                .totalToday(todayLogs.size())
                .okCountToday(okCount)
                .ngCountToday(ngCount)
                .last24hCount(last24hLogs.size())
                .ngCountByDevice(ngByDevice)
                .countBySeverity(bySeverity)
                .build();
    }
}