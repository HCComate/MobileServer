package com.semse.mobile_server.dto;

import lombok.Getter;

@Getter
public class AlertRespondRequest {
    private String response; // ACCEPTED / REJECTED
    private String userId;
    private String deviceId; // 앱이 명시 전달 (alertId 파싱 오류 방지)
}