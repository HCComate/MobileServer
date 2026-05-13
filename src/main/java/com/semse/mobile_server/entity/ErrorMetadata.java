package com.semse.mobile_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorMetadata {

    @Id
    private String errorCode; // "HM-PO-01"

    private String category;    // "하드웨어 · 기계"
    private String errorName;   // "비상 정지 버튼 활성"

    @Enumerated(EnumType.STRING)
    private Severity severity;  // CRITICAL / HIGH / MEDIUM / LOW

    @Column(columnDefinition = "TEXT")
    private String description; // 발생 상황

    @Column(columnDefinition = "TEXT")
    private String solution;    // 대응 방법
}