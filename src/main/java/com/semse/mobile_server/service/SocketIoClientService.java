package com.semse.mobile_server.service;

import com.semse.mobile_server.config.RawLogWebSocketHandler;
import io.socket.client.IO;
import io.socket.client.Socket;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class SocketIoClientService {

    private final RawLogWebSocketHandler rawLogWebSocketHandler;
    private final CriticalAlertService criticalAlertService;

    @Value("${admin.pc.base-url}")
    private String adminBaseUrl;

    private Socket socket;

    @PostConstruct
    public void connect() {
        try {
            IO.Options options = IO.Options.builder()
                    .setTransports(new String[]{"websocket"})
                    .build();

            socket = IO.socket(URI.create(adminBaseUrl), options);

            // mobile_data_feed 이벤트 구독
            socket.on("mobile_data_feed", args -> {
                try {
                    String payload = args[0].toString();
                    System.out.println("mobile_data_feed 수신");
                    rawLogWebSocketHandler.sendRawLog(payload);
                } catch (Exception e) {
                    System.out.println("mobile_data_feed 처리 실패: " + e.getMessage());
                }
            });

            // critical_alert 이벤트 구독
            socket.on("critical_alert", args -> {
                try {
                    String payload = args[0].toString();
                    System.out.println("critical_alert 수신: " + payload);
                    criticalAlertService.handleCriticalAlert(payload);
                } catch (Exception e) {
                    System.out.println("critical_alert 처리 실패: " + e.getMessage());
                }
            });

            // 연결 상태 로그
            socket.on(Socket.EVENT_CONNECT, args ->
                    System.out.println("Socket.IO 연결 성공"));
            socket.on(Socket.EVENT_DISCONNECT, args ->
                    System.out.println("Socket.IO 연결 종료"));
            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    System.out.println("Socket.IO 연결 실패: " + args[0]));

            socket.connect();
            System.out.println("Socket.IO 연결 시도: " + adminBaseUrl);

        } catch (Exception e) {
            System.out.println("Socket.IO 초기화 실패: " + e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
        }
    }
}