GitHub PR의 코드를 리뷰합니다.
사용법: `/review-pr <PR번호>` 또는 현재 브랜치의 PR을 자동으로 감지합니다.

## 진행 순서

1. **PR 정보 가져오기**
   - 인자로 PR 번호가 주어진 경우: `gh pr view <PR번호> --json title,body,files,commits`
   - 인자가 없는 경우: `gh pr view --json title,body,files,commits` (현재 브랜치 기준)
   - PR을 찾을 수 없으면 사용자에게 번호를 물어보세요

2. **변경 diff 가져오기**
   `gh pr diff <PR번호>`

3. **아래 관점으로 코드를 리뷰하세요:**

### 백엔드 코드 리뷰 기준 (Spring Boot)
- **정확성**: 비즈니스 로직이 요구사항과 일치하는가
- **보안**: SQL 인젝션, XSS, 인증/인가 누락 여부, 민감 정보 하드코딩 여부
- **이상 탐지 임계치**: `application.yml` 외부에서 관리되는 임계치 값이 없는가
- **에러 처리**: 에러 응답 형식 (`{ status, message, timestamp }`) 준수 여부
- **AI 연동**: FastAPI 장애 시 fallback 로직이 포함되어 있는가
- **중복 방지**: Redis TTL 기반 중복 티켓 방지 로직이 올바른가
- **타임아웃**: RestTemplate 연결 3초 / 응답 10초 설정 준수 여부
- **날짜 형식**: ISO 8601 형식 사용 여부

### 일반 리뷰 기준
- PR 범위가 이슈와 일치하는가 (과도한 변경 포함 여부)
- 의도하지 않은 파일 포함 여부 (`application-local.yml`, `.env` 등)

4. **리뷰 결과 출력 형식:**

```
## PR 리뷰 결과: <PR 제목>

### 요약
(전반적인 평가 한 줄)

### 필수 수정 (Blocking)
- ...

### 개선 제안 (Non-blocking)
- ...

### 잘된 점
- ...

### 결론
APPROVE / REQUEST CHANGES / COMMENT
```

5. **GitHub에 리뷰 남기기** (사용자 동의 후)
   - `APPROVE` 또는 `REQUEST CHANGES` 중 선택하여 `gh pr review` 로 리뷰를 제출하세요
   - 구체적인 코멘트는 `gh pr review <번호> --comment -b "<내용>"` 으로 추가
