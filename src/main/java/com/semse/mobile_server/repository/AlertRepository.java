package com.semse.mobile_server.repository;

import com.semse.mobile_server.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findAllByOrderByTimestampDesc();
    List<Alert> findByResponseIsNullOrderByTimestampDesc();
}