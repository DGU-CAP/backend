# Backend - AI 기반 쿠버네티스 모니터링 플랫폼

## 프로젝트 소개

Kubernetes 클러스터의 메트릭/로그/이벤트를 실시간 수집하고 이상 탐지 및 AI 분석을 통해 자동으로 티켓을 생성하는 SaaS형 모니터링 플랫폼의 백엔드 서버입니다.

정식 명칭: **클라우드 PaaS 보안을 위한 AI 기반 쿠버네티스 모니터링 플랫폼**

---

## 기술 스택

- Java 17
- Spring Boot 3.2.x
- Gradle
- PostgreSQL / Redis
- Docker / AWS EKS / ECR / SES

---

## 패키지 구조

루트 패키지: `com.dgu.cap`

```
com.dgu.cap
├── anomaly/       # 이상 탐지 (룰 기반)
├── ticket/        # 티켓 생성·관리·이력
├── alert/         # 알람 / SSE
├── metric/        # Prometheus 연동
├── log/           # Loki 연동
├── kubernetes/    # K8s API 연동
├── ai/            # FastAPI AI 서버 연동
└── config/        # 설정 (application.yml 매핑 등)
```

---

## 사전 요구사항

| 도구 | 버전 |
| --- | --- |
| Java | 17 |
| Docker | 24+ |
| Gradle | Wrapper 사용 (별도 설치 불필요) |

---

## 로컬 실행

```bash
./gradlew bootRun
```

---

## Docker 빌드 및 실행

```bash
# 빌드
docker build -t dgu-cap-backend .

# 단독 실행 (DB / Prometheus 없이는 부팅만 확인 가능)
docker run -p 8080:8080 dgu-cap-backend
```

---

## 인프라 연동 (로컬 개발)

본 백엔드는 단독으로 동작하지 않으며 **인프라 레포(Kind 클러스터)** 가 함께 떠 있어야 합니다.

| 의존 컴포넌트 | 제공처 | 로컬 포트 |
| --- | --- | --- |
| Prometheus | 인프라 레포 (Helm) | `9090` |
| Loki | 인프라 레포 (Helm) | `3100` |
| PostgreSQL | 인프라 레포 (Pod) | `5432` |
| Redis | 인프라 레포 (Pod) | `6379` |
| FastAPI AI 서버 | AI 레포 | `8000` |

권장 개발 흐름:

1. 인프라 레포 클론 → Kind 클러스터 기동 → Prometheus / Loki / PostgreSQL / Redis Pod 확인
2. 백엔드는 IntelliJ 에서 `bootRun` 으로 실행 (Pod 로 올리지 않아도 됨)
3. 본격 통합 테스트 단계에서만 백엔드 이미지를 ECR push → Kind pull 로 올림

---

## CI / CD 파이프라인

코드 push 시 GitHub Actions가 다음을 자동 수행합니다.

1. `./gradlew build` 빌드 + 테스트
2. Docker 이미지 빌드
3. AWS ECR 푸시
4. (인프라) Kind / EKS 에서 새 이미지 pull 후 Pod 재기동

ECR 레포지토리:
```
428185450315.dkr.ecr.ap-northeast-2.amazonaws.com/dgu-cap-backend
```

워크플로 위치: `.github/workflows/`

---

## 전체 흐름

1. 스케줄러가 30초마다 Prometheus / Loki / K8s API 호출
2. 룰 기반 이상 탐지 (1차 필터링)
3. 이상 감지 시 FastAPI AI 서버로 데이터 전달
4. FastAPI 분석 결과를 받아 티켓 생성 + 이메일(AWS SES) + SSE 전송
5. React 대시보드에 실시간 표시

---

## 이상 탐지 룰

임계치는 `application.yml`에서 관리하며 **하드코딩 금지**.

| 항목 | 임계치 |
| --- | --- |
| CPU | > 90% |
| 메모리 | > 85% |
| Pod 재시작 | >= 3회 |
| HTTP 에러율 | > 10% |
| K8s 이벤트 | OOMKilled |
| K8s 이벤트 | CrashLoopBackOff |

---

## 티켓

- 심각도: `CRITICAL` / `HIGH` / `MEDIUM` / `LOW`
- 상태: `OPEN` / `IN_PROGRESS` / `RESOLVED` / `CLOSED`
- Redis 중복 방지 (10분 TTL)
- AI 분석 결과 포함
- 조치 이력 저장 (누가 / 언제 / 어떻게)

---

## REST API

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/pods` | Pod 목록 / 상태 |
| GET | `/api/metrics/current` | 현재 메트릭 |
| GET | `/api/metrics/range` | 시계열 메트릭 (그래프용) |
| GET | `/api/logs` | 로그 조회 |
| GET | `/api/tickets` | 티켓 목록 |
| GET | `/api/tickets/{id}` | 티켓 상세 |
| PATCH | `/api/tickets/{id}/status` | 티켓 상태 변경 |
| GET | `/api/tickets/{id}/logs` | 조치 이력 |
| GET | `/api/alerts` | 알람 이력 |
| GET | `/api/topology` | 서비스 토폴로지 |
| GET | `/api/stream` | SSE 연결 |

---

## 환경변수

`application.yml`에서 참조하는 민감 정보는 환경변수 또는 K8s Secret으로 주입합니다.

| 환경변수 | 설명 |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `AWS_ACCESS_KEY_ID` | AWS 자격증명 |
| `AWS_SECRET_ACCESS_KEY` | AWS 자격증명 |
| `AWS_REGION` | AWS 리전 (기본: `ap-northeast-2`) |

---

## 브랜치 전략 및 커밋 컨벤션

**브랜치 네이밍**
```
feat/기능명
fix/버그명
refactor/대상
```

**커밋 메시지**
```
feat: 티켓 생성 API 구현
fix: Prometheus 연동 NPE 수정
refactor: AnomalyService 분리
docs: README 업데이트
```

---

## 개발 규칙

- 임계치 등 환경값은 설정 파일로 관리하고 코드에 **하드코딩 금지**
- API 키 / 비밀번호는 환경변수 또는 K8s Secret 으로 주입 (Git 업로드 금지)
- AI 서버 장애 시 백엔드 단독으로 티켓 생성 가능 (`try/catch`)
- `RestTemplate` 타임아웃: 연결 3초 / 응답 10초
- 날짜 형식: ISO 8601 (예: `2026-04-26T21:30:00`)
- 에러 응답 형식: `{ status, message, timestamp }` 통일
- 필드 네이밍: camelCase (요청/응답 모두)
- 인프라(`infra/kind`) 변경 필요 시 Issue → 브랜치 → PR → 리뷰 → 머지 프로세스 준수
- 챗봇 없음, 자동 대응 없음 (AI 권장 조치 제시까지만)
- Grafana 없음 (대시보드는 React 로 자체 구현)

