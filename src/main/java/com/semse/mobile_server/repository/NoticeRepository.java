package com.semse.mobile_server.repository;

import com.semse.mobile_server.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 최신순 전체 조회
    List<Notice> findAllByOrderByCreatedAtDesc();

    // 날짜 범위 조회 (캘린더 필터용)
    List<Notice> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end
    );

    // 작성자별 조회
    List<Notice> findByAuthorOrderByCreatedAtDesc(String author);
}
