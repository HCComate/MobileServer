package com.semse.mobile_server.controller;

import com.semse.mobile_server.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 전체 조회 — 앱은 response.data가 배열이길 기대하므로 List를 그대로 반환
    // (AdminPC-Server /api/notices로 위임)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }
}
