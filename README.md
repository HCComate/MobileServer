# VisionMate (비전메이트)

반도체 비전 검사 장비의 실시간 상태와 검사 결과를 모바일 환경에 제공하는 Spring Boot 기반 Mobile Server

---

# 프로젝트 소개

VisionMate는 반도체 제조 공정에서 운용되는 비전 검사 장비의 상태와 검사 결과를 실시간으로 확인할 수 있도록 개발된 통합 모니터링 시스템입니다.

기존의 반도체 검사 장비는 특정 PC 또는 관제실에서만 상태를 확인할 수 있어 엔지니어의 이동성과 즉각적인 대응에 한계가 있었습니다.

VisionMate는 모바일 환경에서 장비 상태, 검사 결과, 로그 및 이상 상황을 실시간으로 확인할 수 있도록 하여 현장 대응력과 운영 효율성을 향상시키는 것을 목표로 합니다.

본 저장소는 VisionMate 프로젝트의 Spring Boot 기반 Mobile Server를 담당합니다.

---

# 프로젝트 목표 및 기대 효과

## 실시간 데이터 전달

장비 상태 및 검사 결과 데이터를 모바일 환경에 안정적으로 전달합니다.

## 사용자 인증 및 권한 관리

JWT 기반 인증 시스템을 통해 사용자 권한에 따른 접근 제어를 제공합니다.

## 실시간 알림 처리

장비 이상 상황 발생 시 모바일 앱에 실시간 알림을 전달합니다.

## 통합 API 서버 역할

AdminPC-Server와 Mobile App 사이에서 중간 API 서버 역할을 수행합니다.

---

# 시스템 아키텍처

```text
MockupSoftware
        ↓
AdminPC
        ↓
AdminPC-Server (Flask)
        ↓
MobileServer (Spring Boot)
        ↓
MobileApp (React Native)
```

---

# 주요 기능

## 사용자 인증

* JWT 로그인 인증
* 사용자 권한(Role) 관리

## 장비 모니터링

* 장비 목록 조회
* 장비 상세 정보 조회
* 검사 결과 조회

## 실시간 통신

* WebSocket(STOMP) 기반 실시간 알림
* Socket.IO 기반 AdminPC-Server 연동

## 로그 및 통계

* 장비 로그 조회
* 통계 데이터 조회

## 장비 제어

* Resolve 요청 처리
* 장비 상태 업데이트

---

# 통신 구조

VisionMate MobileServer는 REST API와 WebSocket을 함께 사용하는 Hybrid Architecture를 적용하였습니다.

## REST API

* 로그인 및 인증
* 장비 목록 조회
* 로그 조회
* 통계 조회
* 사용자 정보 관리

## WebSocket (STOMP)

* 실시간 장비 상태 전달
* 실시간 알림 전달
* 장비 이상 이벤트 전달

---

# 기술 스택

## Backend

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA

## Communication

* REST API
* WebSocket (STOMP)
* Socket.IO Client

## Database

* MySQL
* H2 Database

## Development Tools

* IntelliJ IDEA
* Gradle

---

# 프로젝트 구조

```text
MobileServer/
├── config/          # Spring 설정
├── controller/      # REST API Controller
├── service/         # 비즈니스 로직
├── repository/      # JPA Repository
├── entity/          # Entity 클래스
├── dto/             # DTO 클래스
├── websocket/       # WebSocket 관련 클래스
├── security/        # JWT 인증 및 보안 설정
└── socketio/        # AdminPC-Server 연동
```

---

# 개발 환경

| Category  | Technology          |
| --------- | ------------------- |
| OS        | Windows             |
| IDE       | IntelliJ IDEA       |
| Framework | Spring Boot         |
| Database  | MySQL / H2          |
| Network   | Local Wi-Fi Network |

---

# 실행 방법

## 저장소 복제

```bash
git clone https://github.com/HCComate/MobileServer.git
```

## 의존성 설치 및 실행

```bash
./gradlew bootRun
```

또는 IntelliJ IDEA에서 실행 가능합니다.

---

# 실행 전 참고사항

VisionMate는 단독 애플리케이션이 아닌 통합 모니터링 시스템의 일부로 구성되어 있습니다.

실시간 데이터 조회 및 주요 기능을 정상적으로 사용하기 위해서는 아래 구성 요소가 모두 실행되어야 합니다.

* Mock Program
* Admin PC Server (Flask)
* Admin PC
* Mobile Server (Spring Boot)
* Mobile App (React Native)

---

# Contributors

* 홍재민
* 이시형
* 박석준
* 김가현

---

# 작품명

VisionMate
