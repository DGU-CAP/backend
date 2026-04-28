# Backend - AI 기반 쿠버네티스 모니터링 플랫폼

## 프로젝트 소개
Kubernetes 클러스터의 메트릭/로그/이벤트를 실시간 수집하고
이상 탐지 및 AI 분석을 통해 자동으로 티켓을 생성하는
SaaS형 모니터링 플랫폼의 백엔드 서버입니다.

## 기술 스택
- Java 17
- Spring Boot
- Gradle

## 로컬 실행

```bash
./gradlew bootRun
```

## Docker 빌드 및 실행

```bash
# 빌드
docker build -t dgu-cap-backend .

# 실행
docker run -p 8080:8080 dgu-cap-backend
```

## ECR 푸시 (인프라 팀 담당)

코드 작성 후 main 브랜치에 머지하면 GitHub Actions가 자동으로 ECR에 푸시합니다.
(워크플로는 추후 추가 예정)
