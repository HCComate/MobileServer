package com.semse.mobile_server.controller;

import com.semse.mobile_server.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 주요 일정 조회 — 앱은 response.data가 배열이길 기대하므로 List를 그대로 반환
    // (AdminPC-Server /api/events로 위임, month 또는 date 필터)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getEvents(
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "date", required = false) String date) {
        return ResponseEntity.ok(eventService.getEvents(month, date));
    }
}
