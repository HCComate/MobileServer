package com.semse.mobile_server.dto;

import lombok.Getter;

@Getter
public class AlertRespondRequest {
    private String response; // ACCEPTED / REJECTED
    private String userId;
}