package com.semse.mobile_server.entity;

/**
 * 장비의 현재 동작 상태를 나타냅니다.
 *
 * <ul>
 *   <li>RUN    - 장비가 가동 중이며 검사 데이터를 전송하고 있습니다.</li>
 *   <li>IDLE   - 장비 전원은 켜져 있으나 현재 검사 데이터를 전송하지 않는 대기 상태입니다.
 *               (AdminPC-Server 의 STANDBY 상태도 MobileServer 에서 IDLE 로 변환하여 저장합니다.)</li>
 *   <li>LOCKED - CRITICAL 오류가 발생하여 장비가 강제 잠금된 상태입니다.</li>
 *   <li>STOP   - 장비 전원이 꺼져 있거나 사용자에 의해 명시적으로 정지된 상태입니다.</li>
 *   <li>ERROR  - CRITICAL 이 아닌 오류가 발생했으나 가동은 유지되는 상태입니다.</li>
 * </ul>
 */
public enum MachineStatus {
    RUN, STANDBY, IDLE, LOCKED, STOP, ERROR
}
