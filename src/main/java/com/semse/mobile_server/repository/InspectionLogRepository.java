package com.semse.mobile_server.repository;

import com.semse.mobile_server.entity.InspectionLog;
import com.semse.mobile_server.entity.MachineStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InspectionLogRepository extends JpaRepository<InspectionLog, Long> {

    // "최신" 판정은 timestamp가 아닌 id(저장 순서) 기준.
    // timestamp는 장비 페이로드 값이라 미래/엉터리 값이 섞일 수 있어(예: 미래 날짜 시드 데이터)
    // timestamp DESC로 뽑으면 오래된 쓰레기 로그가 "최신"으로 잡힌다. id는 단조 증가라 안전.
    // (목록 findLatestPerDevice가 MAX(id)를 쓰므로 상세도 id 기준이어야 일치한다.)
    Optional<InspectionLog> findTopByDeviceIdOrderByIdDesc(String deviceId);

    List<InspectionLog> findByMachineStatusOrderByTimestampDesc(MachineStatus status);

    List<InspectionLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    List<InspectionLog> findTop50ByOrderByIdDesc();

    // 저장순(id) 최근 N건 — limit을 호출부에서 지정(전체 로그 화면용).
    // 50건 고정이면 정상 로그 폭주에 ERROR/이벤트 로그가 밀려 앱까지 도달 못 하므로
    // 앱이 요청하는 limit(기본 200)을 반영한다.
    List<InspectionLog> findByOrderByIdDesc(Pageable pageable);

    // 이벤트(오류) 로그만 저장순(id)으로 — 이벤트 로그 화면용.
    // 초당 50건 폭주에선 "최근 N건"을 받아 앱에서 필터하면 ERROR가 정상로그에 묻혀
    // 누락된다. H2엔 /api/logs/after 증분 폴링으로 모든 로그가 빠짐없이 적재되므로,
    // 서버에서 ERROR/LOCKED만 골라 내리면 폭주와 무관하게 항상 이벤트가 표시된다.
    // (CRITICAL은 저장 시 LOCKED로 변환되므로 둘 다 포함)
    List<InspectionLog> findByMachineStatusInOrderByIdDesc(
            java.util.Collection<MachineStatus> statuses, Pageable pageable);

    List<InspectionLog> findByTimestampAfter(LocalDateTime timestamp);

    // 장비별 최신 로그 1건씩 조회 (id MAX 기준 → H2에서 안정적)
    @Query("SELECT l FROM InspectionLog l WHERE l.id IN " +
           "(SELECT MAX(l2.id) FROM InspectionLog l2 GROUP BY l2.deviceId)")
    List<InspectionLog> findLatestPerDevice();
}