package com.semse.mobile_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class VisionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String result;
    private String defectType;
    private Double confidence;
    private String inspectionArea;
    private String imageUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_log_id")
    private InspectionLog inspectionLog;
}