package com.semse.mobile_server.repository;

import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.entity.MachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
    List<InspectionLog> findTop20ByOrderByTimestampDesc();
}