package com.semse.mobile_server.service;

import com.semse.mobile_server.entity.Notice;
import com.semse.mobile_server.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 전체 조회
    public List<Notice> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc();
    }

    // 단건 조회
    public Notice getNoticeById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다: " + id));
    }

    // 날짜별 조회
    public List<Notice> getNoticesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return noticeRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    // 생성
    public Notice createNotice(Notice notice) {
        return noticeRepository.save(notice);
    }

    // 수정
    public Notice updateNotice(Long id, Notice updated) {
        Notice notice = getNoticeById(id);
        notice.setTitle(updated.getTitle());
        notice.setContent(updated.getContent());
        notice.setAuthor(updated.getAuthor());
        return noticeRepository.save(notice);
    }

    // 삭제
    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }
}
