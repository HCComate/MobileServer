package com.semse.mobile_server.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatisticsResponse {
    private int totalDevices;
    private int runningDevices;
    private int errorDevices;
    private int totalInspections;
    private int okCount;
    private int ngCount;
    private double ngRate;
    private int errorCount;
}