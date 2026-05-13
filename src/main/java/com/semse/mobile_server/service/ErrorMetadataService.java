package com.semse.mobile_server.service;

import com.semse.mobile_server.dto.ErrorMetadataResponse;
import com.semse.mobile_server.entity.ErrorMetadata;
import com.semse.mobile_server.repository.ErrorMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErrorMetadataService {

    private final ErrorMetadataRepository errorMetadataRepository;

    public List<ErrorMetadataResponse> getAllErrors() {
        return errorMetadataRepository.findAll().stream()
                .map(ErrorMetadataResponse::from)
                .collect(Collectors.toList());
    }

    public ErrorMetadataResponse getErrorByCode(String code) {
        ErrorMetadata error = errorMetadataRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("에러코드를 찾을 수 없습니다: " + code));
        return ErrorMetadataResponse.from(error);
    }
}