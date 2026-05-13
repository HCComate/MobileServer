package com.semse.mobile_server.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlertEvent {
    private String alertId;
    private String deviceId;
    private String errorCode;
    private String errorMsg;
    private String severity;
    private String timestamp;
}