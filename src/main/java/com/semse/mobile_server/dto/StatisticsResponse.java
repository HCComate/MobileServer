package com.semse.mobile_server.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class StatisticsResponse {
    private long totalToday;
    private long okCountToday;
    private long ngCountToday;
    private long last24hCount;
    private Map<String, Long> ngCountByDevice;
    private Map<String, Long> countBySeverity;
}