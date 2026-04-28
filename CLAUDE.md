# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# 로컬 실행
./gradlew bootRun

# 빌드 (테스트 제외)
./gradlew bootJar -x test

# 전체 빌드 + 테스트
./gradlew build

# 단일 테스트 클래스 실행
./gradlew test --tests "com.dgu.cap.TicketServiceTest"

# Docker 빌드
docker build -t dgu-cap-backend .
```

## 아키텍처

**전체 흐름**
스케줄러(30초) → Prometheus/Loki/K8s API 수집 → 룰 기반 이상 탐지 → FastAPI POST /analyze → 티켓 생성 + SES 이메일 + SSE 전송 → React 대시보드

**패키지 역할**
- `metric/` — Prometheus 연동, 메트릭 수집
- `log/` — Loki 연동, 로그 수집
- `kubernetes/` — K8s API 연동, Pod/이벤트 수집
- `anomaly/` — 룰 기반 이상 탐지 (임계치는 application.yml에서만 관리)
- `ai/` — FastAPI 서버 연동 (장애 시 백엔드 단독 티켓 생성 fallback 필수)
- `ticket/` — 티켓 CRUD, 조치 이력, 메트릭 스냅샷
- `alert/` — SSE 스트림, 이메일 알람
- `config/` — RestTemplate, Redis, 외부 설정 바인딩

**DB 테이블**
- `incident_ticket` — 티켓 본문 (severity: CRITICAL/HIGH/MEDIUM/LOW, status: OPEN/IN_PROGRESS/RESOLVED/CLOSED)
- `ticket_action_log` — 티켓 조치 이력
- `ticket_metric_snapshot` — 티켓 생성 시점 메트릭 스냅샷

**중복 방지**: 동일 Pod 이상 탐지 시 Redis TTL 10분으로 중복 티켓 생성 차단

**SSE 이벤트 타입**: `NEW_ALERT`, `METRIC_UPDATE`, `POD_STATUS`, `TICKET_UPDATED`

## 개발 규칙

- 이상 탐지 임계치는 `application.yml`에서만 관리 (`@ConfigurationProperties` 또는 `@Value` 사용)
- RestTemplate 타임아웃: 연결 3초 / 응답 10초
- 에러 응답 형식: `{ status, message, timestamp }`
- 날짜 형식: ISO 8601
- AI 서버 장애 시 fallback으로 백엔드 단독 티켓 생성 가능해야 함

## 외부 연동 설정 (application.yml)

```yaml
prometheus:
  url: http://localhost:9090
loki:
  url: http://localhost:3100
ai:
  url: http://localhost:8000
anomaly:
  threshold:
    cpu: 90.0
    memory: 85.0
    restart: 3
    error-rate: 10.0
```

## 배포

main 브랜치 머지 시 GitHub Actions가 ECR(`428185450315.dkr.ecr.ap-northeast-2.amazonaws.com/dgu-cap-backend`)에 자동 푸시.
