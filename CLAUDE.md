# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

AI 기반 Kubernetes 모니터링 플랫폼.
Prometheus/Loki/K8s API로 메트릭·로그·이벤트를 수집하고, 룰 기반 이상 탐지 후 AI 분석을 통해 티켓 자동 생성 및 실시간 알람을 제공.

**전체 흐름**
스케줄러(30초) → Prometheus/Loki/K8s API 수집 → 룰 기반 이상 탐지 → FastAPI POST /analyze → 티켓 생성 + SES 이메일 + SSE 전송 → React 대시보드

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

# 로컬 테스트 (DB/Redis 환경변수 필요)
SPRING_PROFILES_ACTIVE=local ./gradlew test

# Docker 빌드
docker build -t dgu-cap-backend .
```

## Git 협업 워크플로

**이슈 없는 PR은 올리지 않는다.**

```
이슈 생성 → 브랜치 생성 → 작업 → PR 생성 → 리뷰 → dev 머지 → main 머지
```

- 브랜치: `<type>/#<이슈번호>-<설명>` (예: `feat/#3-config-package`)
- base 브랜치: **dev** (main에 직접 PR 금지)
- 커밋: `<type>: <내용>` — type: `feat` / `fix` / `chore` / `docs`
- **PR은 직접 머지하지 않는다. 생성 후 사용자가 직접 머지.**

### Claude Code 슬래시 커맨드

| 커맨드 | 설명 |
|---|---|
| `/new-issue` | GitHub 이슈 생성 |
| `/new-pr` | PR 생성 (이슈 연결 필수, base: dev) |
| `/review-pr <번호>` | PR 코드리뷰 |

## 패키지 구조

```
com.dgu.cap
├── CapApplication.java
├── config/
│   ├── AppConfig.java                  # RestTemplate (연결 3초 / 응답 10초)
│   ├── CorsConfig.java
│   └── RedisConfig.java
├── anomaly/
│   ├── AnomalyDetectionService.java    # 30초 스케줄러 + 룰 기반 탐지
│   ├── AnomalyThresholdProperties.java # application.yml 임계치 바인딩
│   └── AnomalyType.java
├── ticket/
│   ├── Ticket.java                     # incident_ticket 테이블
│   ├── TicketActionLog.java            # ticket_action_log 테이블
│   ├── TicketMetricSnapshot.java       # ticket_metric_snapshot 테이블
│   ├── TicketRepository.java
│   ├── TicketActionLogRepository.java
│   ├── TicketMetricSnapshotRepository.java
│   ├── TicketService.java
│   └── TicketController.java
├── alert/
│   ├── AlertService.java               # AWS SES 이메일 발송
│   ├── SseService.java                 # SSE 연결 관리 (CopyOnWriteArrayList)
│   └── SseController.java             # GET /api/stream
├── metric/
│   ├── PrometheusService.java          # Prometheus HTTP API 연동
│   ├── MetricController.java
│   ├── MetricPoint.java                # 시계열 데이터 포인트
│   └── CurrentMetric.java
├── log/
│   ├── LokiService.java                # Loki HTTP API 연동
│   └── LogController.java
├── kubernetes/
│   ├── KubernetesService.java          # K8s Java Client 연동
│   ├── PodController.java
│   ├── PodInfo.java
│   └── PodEvent.java
└── ai/
    ├── AiService.java                  # FastAPI POST /analyze + fallback
    ├── MetricsCollectionScheduler.java # 60초 스케줄러 — 전체 Pod 메트릭 수집 후 AI 전송
    ├── PodData.java                    # AI /analyze 요청 DTO
    ├── AiResult.java                   # AI 응답 DTO
    ├── MetricsData.java                # 메트릭 시계열 묶음 DTO
    └── MetricsSendRequest.java         # AI /metrics 요청 DTO
```

## 아키텍처

**DB 테이블**
- `incident_ticket` — severity: CRITICAL/HIGH/MEDIUM/LOW / status: OPEN/IN_PROGRESS/RESOLVED/CLOSED
- `ticket_action_log` — 조치 이력
- `ticket_metric_snapshot` — 티켓 생성 시점 메트릭 스냅샷

**Redis 중복 방지**
- Key: `ticket:{podName}:{anomalyType}` / Value: `"1"` / TTL: 10분

**SSE 이벤트 타입**: `NEW_ALERT`, `METRIC_UPDATE`, `POD_STATUS`, `TICKET_UPDATED`

**이상 탐지 룰**
- CPU > 90% → CPU_HIGH
- 메모리 > 85% → MEMORY_HIGH
- 재시작 >= 3회 → POD_RESTART
- 에러율 > 10% → ERROR_RATE_HIGH
- OOMKilled → OOM_KILLED
- CrashLoopBackOff → CRASH_LOOP

## 개발 규칙

- 임계치: `application.yml`에서만 관리 (`@ConfigurationProperties` 사용, 하드코딩 절대 금지)
- AI 장애: try/catch로 처리, 백엔드 단독 티켓 생성 fallback 필수
- RestTemplate 타임아웃: 연결 3초 / 응답 10초 (AppConfig에서 설정)
- 에러 응답 형식: `{ status, message, timestamp }`
- 날짜 형식: ISO 8601
- 비밀번호/API키: 하드코딩 금지, 환경변수로만 주입
- SseEmitter 목록: `CopyOnWriteArrayList` 사용 (멀티스레드 안전)
- `@Transactional` 범위: 이메일 등 외부 API 호출은 트랜잭션 밖에서 처리
- N+1 문제: 목록 조회 시 주의, 필요 시 In 쿼리로 해결

## 환경변수 (K8s backend deployment에서 주입)

| 변수 | 설명 |
|---|---|
| `DB_URL` | PostgreSQL 접속 URL |
| `DB_USERNAME` | DB 계정명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 (기본값 6379) |
| `PROMETHEUS_URL` | Prometheus URL |
| `LOKI_URL` | Loki URL |
| `AI_URL` | FastAPI AI 서버 URL |
| `ALERT_EMAIL` | 알람 수신 이메일 (기본값 admin@example.com) |
| `ANOMALY_THRESHOLD_CPU` | CPU 임계치 (기본값 90.0) |
| `ANOMALY_THRESHOLD_MEMORY` | 메모리 임계치 (기본값 85.0) |
| `ANOMALY_THRESHOLD_RESTART` | 재시작 임계치 (기본값 3) |
| `ANOMALY_THRESHOLD_ERROR_RATE` | 에러율 임계치 (기본값 10.0) |

## 로컬 개발 방식

### IntelliJ 직접 실행 (빠른 개발)
```bash
kubectl port-forward svc/postgres 5432:5432
kubectl port-forward svc/redis 6379:6379
kubectl port-forward svc/kube-prometheus-stack-prometheus 9090:9090 -n monitoring
kubectl port-forward svc/loki-gateway 3100:80 -n monitoring
```
`application-local.yml`에 위 주소 설정 후 `--spring.profiles.active=local`로 실행.

### K8s 환경 테스트
```bash
git push → GitHub Actions → ECR push
→ .\kind\pull-and-load.ps1 -App backend
→ kubectl port-forward svc/backend 8080:8080
```

## 배포

- **dev 머지** → CI (빌드 + 테스트)
- **main 머지** → CI + ECR(`428185450315.dkr.ecr.ap-northeast-2.amazonaws.com/dgu-cap-backend`) 자동 푸시
