# 운영 배포 가이드

## 1. 사전 조건

- Java 21 런타임
- 애플리케이션 서버에서 접근 가능한 MySQL
- TLS를 종료하고 `X-Forwarded-*` 헤더를 전달하는 reverse proxy 또는 load balancer
- 스키마가 반영된 DB와 배포 직전 백업

운영 기본값은 `ddl-auto=none`입니다. DB 스키마는 배포 전에 별도로 반영해야 하며 운영에서 `JPA_DDL_AUTO=create`, `create-drop`, `update`를 사용하지 않습니다.

문서 일일 통계 기능을 포함한 버전을 처음 배포할 때 애플리케이션 기동 전에 다음 스크립트를 적용합니다. 스크립트는 테이블을 생성하고 현재 문서 데이터 기준 초기 스냅샷을 멱등 반영합니다.

```bash
mysql -h db-host -u grade_stock_app -p new_toy < db/document_daily_stats.sql
mysql -h db-host -u grade_stock_app -p new_toy < db/front_content_view_event.sql
```

`front_content_view_event`는 공개 콘텐츠 조회를 문서·방문자·날짜별로 중복 제거합니다. 애플리케이션 배포 전에 테이블을 생성해야 하며 기존 `document.view_count` 값에는 영향을 주지 않습니다.

## 2. 빌드

```bash
./gradlew clean test bootJar
```

실행 파일은 다음 경로에 생성됩니다.

- `Front/build/libs/Front-0.0.1-SNAPSHOT.jar`
- `admin/build/libs/admin-0.0.1-SNAPSHOT.jar`
- `batch/build/libs/batch-0.0.1-SNAPSHOT.jar`

`common`은 실행 애플리케이션이 아니라 각 모듈에 포함되는 라이브러리 JAR입니다.

## 3. 공통 환경변수

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://db-host:3306/new_toy?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export DB_USERNAME='grade_stock_app'
export DB_PASSWORD='replace-with-secret-manager-value'
export DB_MAX_POOL_SIZE=20
```

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 운영 프로파일에서 필수입니다. 비밀번호는 셸 파일이나 저장소에 기록하지 않고 배포 플랫폼의 secret 기능으로 주입합니다.

## 4. 최초 관리자

`admin_user` 테이블이 비어 있는 최초 배포에서만 다음 값을 지정합니다.

```bash
export ADMIN_BOOTSTRAP_LOGIN_ID='ops.master'
export ADMIN_BOOTSTRAP_PASSWORD='replace-with-at-least-12-characters'
export ADMIN_BOOTSTRAP_NAME='운영 총괄'
```

빈 DB에서 값이 누락되면 관리자 애플리케이션은 의도적으로 기동에 실패합니다. 계정이 생성된 다음 배포부터 bootstrap secret은 제거할 수 있습니다. 관리자 비밀번호는 PBKDF2로 저장되며 기존 평문 계정은 첫 로그인 시 자동 전환됩니다.

관리자 세션은 기본 30분이며 쿠키는 운영에서 `Secure`, `HttpOnly`, `SameSite=Strict`로 발급됩니다. 따라서 관리자 서비스는 반드시 HTTPS로 제공해야 합니다.

## 5. 실행

```bash
SERVER_PORT=8080 java -jar Front/build/libs/Front-0.0.1-SNAPSHOT.jar
SERVER_PORT=9090 java -jar admin/build/libs/admin-0.0.1-SNAPSHOT.jar
BATCH_SERVER_PORT=9091 java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar
```

배치는 기본적으로 업무 스케줄이 비활성화됩니다. 문서 일일 통계는 전체 문서를 게시판 유형별로 한 번에 집계하고 `날짜 + 범위` 유니크 키로 멱등 갱신합니다. 중복 실행 부하를 피하기 위해 활성 배치 인스턴스는 1개만 운영하고 아래 값을 명시합니다.

```bash
export BATCH_DOCUMENT_STATS_ENABLED=true
export BATCH_DOCUMENT_STATS_CRON='0 */10 * * * *'
```

관리자는 콘텐츠 목록의 `문서 일일 통계` 영역과 `/api/admin/content/stats/daily`에서 최신 TOTAL·게시판별 스냅샷을 확인할 수 있습니다. 같은 화면의 `프론트 조회 분석` 영역과 `/api/admin/content/stats/views?boardType=NOTICE&days=7`에서는 실제 조회 이벤트의 7·14·30일 추이, 순 방문자, 상위 콘텐츠를 확인합니다.

## 6. 배포 후 확인

각 애플리케이션은 다음 엔드포인트를 제공합니다.

- `/health/live`: 프로세스 생존 여부
- `/health/ready`: DB 연결을 포함한 요청 처리 준비 여부

```bash
FRONT_URL=https://service.example.com \
ADMIN_URL=https://admin.example.com \
BATCH_URL=http://batch.internal:9091 \
FRONT_DETAIL_PRODUCT_ID=12 \
ADMIN_SMOKE_LOGIN_ID=smoke-admin \
ADMIN_SMOKE_PASSWORD='replace-with-secret' \
./scripts/smoke-test.sh
```

`FRONT_DETAIL_PRODUCT_ID`에는 운영 DB에 존재하는 대표 상품 ID를 지정합니다. 프론트 smoke test는 카탈로그와 상세 화면/API, 미등록 상품의 `F002/404` 오류 계약을 확인합니다. 관리자 smoke test는 비로그인 화면 요청의 로그인 리다이렉트와 `/api/admin/**`의 `401`을 확인합니다. `ADMIN_SMOKE_LOGIN_ID`, `ADMIN_SMOKE_PASSWORD`를 제공하면 실제 로그인, 대시보드, 문서 일일 통계·프론트 조회 분석 API, 로그아웃과 `Clear-Site-Data`까지 추가 검증하며 값은 로그에 출력하지 않습니다.

세 애플리케이션은 응답의 `X-Request-Id`를 공통 장애 추적 키로 사용합니다. 프록시가 안전한 요청 ID를 전달하면 그대로 유지하고, 없거나 형식이 잘못된 경우 애플리케이션이 새 ID를 생성합니다. 요청 헤더 한도는 기본 16KB이며 필요한 경우 `SERVER_MAX_HTTP_REQUEST_HEADER_SIZE`로 조정합니다.

관리자 세션 유효 시간은 `ADMIN_SESSION_TIMEOUT` 설정을 그대로 사용합니다. 로그아웃 시 관리자 서브도메인의 캐시, 쿠키, 브라우저 저장소를 제거하므로 운영 작업 데이터가 공용 브라우저에 남지 않습니다.

## 7. 운영 및 롤백

- load balancer는 `/health/ready`가 `200`인 인스턴스에만 트래픽을 전달합니다.
- 종료 시 Spring graceful shutdown이 진행되므로 프로세스 종료 유예 시간을 30초 이상 둡니다.
- 배포 실패 시 이전 JAR로 롤백하되, DB 변경이 포함됐다면 사전에 준비한 하위 호환 롤백 절차를 따릅니다.
- `/health/ready` 실패, 로그인 불가, 주요 목록 API의 반복적인 `5xx`가 발생하면 신규 버전 트래픽을 중단합니다.
