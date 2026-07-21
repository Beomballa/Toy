# 운영 배포 가이드

## 1. 사전 조건

- Java 21 런타임
- 애플리케이션 서버에서 접근 가능한 MySQL
- TLS를 종료하고 `X-Forwarded-*` 헤더를 전달하는 reverse proxy 또는 load balancer
- 스키마가 반영된 DB와 배포 직전 백업

운영 기본값은 `ddl-auto=none`입니다. DB 스키마는 배포 전에 별도로 반영해야 하며 운영에서 `JPA_DDL_AUTO=create`, `create-drop`, `update`를 사용하지 않습니다.

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

배치는 기본적으로 업무 스케줄이 비활성화됩니다. 현재 문서 집계는 heartbeat만 구현되어 있으므로 운영에서 활성화하지 않습니다. 향후 집계 로직이 완성되면 배치 인스턴스를 1개만 실행하고 아래 값을 명시합니다.

```bash
export BATCH_DOCUMENT_STATS_ENABLED=true
export BATCH_DOCUMENT_STATS_CRON='0 */10 * * * *'
```

## 6. 배포 후 확인

각 애플리케이션은 다음 엔드포인트를 제공합니다.

- `/health/live`: 프로세스 생존 여부
- `/health/ready`: DB 연결을 포함한 요청 처리 준비 여부

```bash
FRONT_URL=https://service.example.com \
ADMIN_URL=https://admin.example.com \
BATCH_URL=http://batch.internal:9091 \
FRONT_DETAIL_PRODUCT_ID=12 \
./scripts/smoke-test.sh
```

`FRONT_DETAIL_PRODUCT_ID`에는 운영 DB에 존재하는 대표 상품 ID를 지정합니다. 프론트 smoke test는 카탈로그와 상세 화면/API, 미등록 상품의 `F002/404` 오류 계약을 확인합니다. 관리자 smoke test는 비로그인 화면 요청의 로그인 리다이렉트와 `/api/admin/**`의 `401`도 함께 확인합니다. 이후 최고 관리자 계정으로 로그인해 대시보드, 상품 목록, 주문 목록, 시스템 설정을 한 번씩 조회합니다.

## 7. 운영 및 롤백

- load balancer는 `/health/ready`가 `200`인 인스턴스에만 트래픽을 전달합니다.
- 종료 시 Spring graceful shutdown이 진행되므로 프로세스 종료 유예 시간을 30초 이상 둡니다.
- 배포 실패 시 이전 JAR로 롤백하되, DB 변경이 포함됐다면 사전에 준비한 하위 호환 롤백 절차를 따릅니다.
- `/health/ready` 실패, 로그인 불가, 주요 목록 API의 반복적인 `5xx`가 발생하면 신규 버전 트래픽을 중단합니다.
