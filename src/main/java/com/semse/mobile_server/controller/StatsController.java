package com.semse.mobile_server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 장비 검사 통계( /api/stats/daily|weekly|monthly|yearly )를 AdminPC-Server로 중계한다.
 *
 * <p>통계 집계는 AdminPC-Server(전체 로그 보유)만 수행하며 MobileServer는 자체
 * H2(폴링분)만 갖고 있어 집계가 불가능하다. 앱은 MobileServer(8080)만 바라보므로
 * alerts/devices와 동일하게 여기서 AdminPC로 위임한다. (이 컨트롤러가 없으면 앱의
 * /api/stats/daily 호출이 404 → "데이터를 불러올 수 없습니다.")</p>
 *
 * <p>AdminPC 응답(JSON)을 그대로 통과시킨다(앱이 response.data를 통계 객체로 직접 사용).</p>
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final RestTemplate restTemplate;

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    @GetMapping(value = "/{period}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> proxyStats(@PathVariable String period, HttpServletRequest request) {
        String query = request.getQueryString();
        String url = adminBaseUrl + "/api/stats/" + period + (query != null ? "?" + query : "");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", "capstone2026");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return ResponseEntity.status(resp.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resp.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // AdminPC가 4xx/5xx로 응답한 경우 상태/본문 그대로 전달
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("[StatsController] 통계 위임 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"통계 서버(AdminPC) 연결 실패\"}");
        }
    }
}
