package com.semse.mobile_server.repository;

import com.semse.mobile_server.entity.ErrorMetadata;
import com.semse.mobile_server.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorMetadataRepository extends JpaRepository<ErrorMetadata, String> {

    List<ErrorMetadata> findBySeverity(Severity severity);
    List<ErrorMetadata> findByCategory(String category);
}