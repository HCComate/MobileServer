package com.semse.mobile_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class StatusInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String msg;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String direction;
    private String partLocation;
    private Boolean isCaptureRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_log_id")
    private InspectionLog inspectionLog;
}