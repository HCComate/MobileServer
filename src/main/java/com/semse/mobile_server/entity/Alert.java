package com.semse.mobile_server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    private String alertId;

    private String deviceId;
    private String errorCode;
    private String errorMsg;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private LocalDateTime timestamp;

    private String response;       // ACCEPTED / REJECTED / null
    private String respondedUserId;
    private LocalDateTime respondedAt;
}