package com.semse.mobile_server.controller;

import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.entity.Notice;
import com.semse.mobile_server.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<Notice>>> getAllNotices() {
        List<Notice> notices = noticeService.getAllNotices();
        return ResponseEntity.ok(ApiResponse.ok(notices));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Notice>> getNotice(@PathVariable Long id) {
        Notice notice = noticeService.getNoticeById(id);
        return ResponseEntity.ok(ApiResponse.ok(notice));
    }

    // 날짜별 조회
    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<Notice>>> getNoticesByDate(
            @PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Notice> notices = noticeService.getNoticesByDate(localDate);
        return ResponseEntity.ok(ApiResponse.ok(notices));
    }

    // 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Notice>> createNotice(@RequestBody Notice notice) {
        Notice created = noticeService.createNotice(notice);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Notice>> updateNotice(
            @PathVariable Long id, @RequestBody Notice notice) {
        Notice updated = noticeService.updateNotice(id, notice);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(ApiResponse.ok("삭제 완료"));
    }
}
