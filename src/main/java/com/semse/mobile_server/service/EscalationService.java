package com.semse.mobile_server.service;

import com.semse.mobile_server.config.AlertWebSocketHandler;
import com.semse.mobile_server.dto.AlertEvent;
import com.semse.mobile_server.entity.Alert;
import com.semse.mobile_server.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class EscalationService {

    private final AlertWebSocketHandler alertWebSocketHandler;
    private final AlertRepository alertRepository;

    // 방치 타임아웃: 20초
    private static final int TIMEOUT_SECONDS = 20;

    // alertId → 타이머 Future (수락/거절 시 타이머 취소용)
    private final Map<String, ScheduledFuture<?>> timerMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * 에스컬레이션 타이머 시작
     * 20초 안에 수락/거절 없으면 재민이 서버에서 다음 target_user_id로 새 이벤트 발생
     * 가현이 서버는 타이머 만료 시 로그만 남기고 대기 (재민이가 다음 이벤트 쏴줌)
     */
    public void startEscalationTimer(String alertId, String deviceId,
                                     String errorCode, String errorMsg, String timestamp) {
        // 기존 타이머 있으면 취소
        cancelTimer(alertId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                // DB에서 해당 alert 확인
                Alert alert = alertRepository.findById(alertId).orElse(null);

                // 이미 수락/거절된 경우 무시
                if (alert != null && alert.getResponse() != null) {
                    System.out.println("[Escalation] 이미 응답됨 - alertId: " + alertId);
                    return;
                }

                // 방치로 판단 → 로그 출력 (재민이 서버가 다음 target_user_id로 새 이벤트 발생)
                System.out.println("[Escalation] 방치 타임아웃 - alertId: " + alertId
                        + ", deviceId: " + deviceId + " → 재민이 서버에서 다음 유저로 에스컬레이션 예정");

                timerMap.remove(alertId);

            } catch (Exception e) {
                System.out.println("[Escalation] 타이머 처리 실패: " + e.getMessage());
            }
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);

        timerMap.put(alertId, future);
        System.out.println("[Escalation] 타이머 시작 - alertId: " + alertId + " (" + TIMEOUT_SECONDS + "초)");
    }

    /**
     * 수락/거절 시 타이머 취소
     */
    public void cancelTimer(String alertId) {
        ScheduledFuture<?> future = timerMap.remove(alertId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            System.out.println("[Escalation] 타이머 취소 - alertId: " + alertId);
        }
    }
}