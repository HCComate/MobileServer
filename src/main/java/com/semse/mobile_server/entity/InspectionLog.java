package com.semse.mobile_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class InspectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // header
    private String deviceId;
    private String batchId;
    private String modelName;

    // body
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    private MachineStatus machineStatus;

    // status_info (1:N)
    @OneToMany(mappedBy = "inspectionLog", cascade = CascadeType.ALL)
    private List<StatusInfo> statusInfos = new ArrayList<>();

    // vision_result (1:1)
    @OneToOne(mappedBy = "inspectionLog", cascade = CascadeType.ALL)
    private VisionResult visionResult;

    // sensor data
    private Double temperature;
    private Double vibrationX;
    private Double vibrationY;
    private Double illumination;
    private Double humidity;

    private LocalDateTime timestamp;
    private LocalDateTime receivedAt;
}