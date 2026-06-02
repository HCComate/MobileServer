package com.semse.mobile_server.repository;

import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.entity.MachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InspectionLogRepository extends JpaRepository<InspectionLog, Long> {

    Optional<InspectionLog> findTopByDeviceIdOrderByTimestampDesc(String deviceId);

    List<InspectionLog> findByMachineStatusOrderByTimestampDesc(MachineStatus status);

    List<InspectionLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    List<InspectionLog> findTop50ByOrderByTimestampDesc();

    List<InspectionLog> findByTimestampAfter(LocalDateTime timestamp);

    // 장비별 최신 로그 1건씩 조회 (id MAX 기준 → H2에서 안정적)
    @Query("SELECT l FROM InspectionLog l WHERE l.id IN " +
           "(SELECT MAX(l2.id) FROM InspectionLog l2 GROUP BY l2.deviceId)")
    List<InspectionLog> findLatestPerDevice();
}