package com.semse.mobile_server.controller;

import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.dto.ErrorMetadataResponse;
import com.semse.mobile_server.service.ErrorMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
public class ErrorMetadataController {

    private final ErrorMetadataService errorMetadataService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ErrorMetadataResponse>>> getAllErrors() {
        return ResponseEntity.ok(ApiResponse.ok(errorMetadataService.getAllErrors()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<ErrorMetadataResponse>> getError(
            @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(errorMetadataService.getErrorByCode(code)));
    }
}