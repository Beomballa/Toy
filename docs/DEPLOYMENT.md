# 운영 배포 가이드

## 1. 사전 조건

- Java 21 런타임
- 애플리케이션 서버에서 접근 가능한 MySQL
- TLS를 종료하고 `X-Forwarded-*` 헤더를 전달하는 reverse proxy 또는 load balancer
- 스키마가 반영된 DB와 배포 직전 백업

운영 기본값은 `ddl-auto=none`입니다. DB 스키마는 배포 전에 버전 실행기로 반영하며 운영에서 `JPA_DDL_AUTO=create`, `create-drop`, `update`를 사용하지 않습니다.

빈 DB와 기존 DB 모두 애플리케이션 기동 전에 다음 명령을 실행합니다. 빈 DB에는 `db/baseline_schema.sql`을 먼저 적용하고, 기존 DB에는 기준 버전을 등록한 뒤 미적용 증분 SQL만 실행합니다.

```bash
DB_HOST=db-host \
DB_NAME=new_toy \
DB_USERNAME=grade_stock_app \
DB_PASSWORD='secret' \
./scripts/migrate-db.sh
```

적용된 버전과 SHA-256 체크섬은 `schema_migration`에 기록됩니다. 이미 적용된 SQL 파일의 내용이 변경되면 실행기는 배포를 중단하므로 기존 파일을 수정하지 말고 새 버전 SQL을 추가해야 합니다.
기존 DB는 기준 스키마의 26개 테이블이 모두 존재할 때만 기준 버전을 기록합니다. 일부 테이블이 누락된 환경에서는 자동으로 기준 SQL을 덮어쓰지 않고 배포를 중단하므로, 백업과 누락 원인을 확인한 뒤 별도 복구해야 합니다.

`request_rate_limit_bucket`은 모든 애플리케이션 인스턴스가 관리자 로그인과 주문조회 제한 상태를 공유하는 운영 보안 테이블입니다.

`front_auth.sql`은 고객 이메일을 로그인 식별자로 고정하고 필수 계정 필드와 이메일 유일키를 적용합니다. 적용 전 공백·중복 이메일을 정리해야 하며 기존 평문 비밀번호는 고객이 로그인할 때 PBKDF2로 전환됩니다.

`front_member_product_activity`는 로그인 회원의 관심·비교·최근 본·숨김 상품을 저장합니다. 회원·종류·상품 유일키와 회원·종류·최신순 인덱스를 사용하며, 로컬 브라우저 활동은 최초 로그인 시 종류별 저장 한도 안에서 서버 데이터와 병합됩니다.

`admin_system_setting_history`는 설정 변경 감사 이력 테이블만 생성하며 운영 데이터는 삽입하지 않습니다. 로컬 화면용 예시는 `local` 프로파일의 시더가 담당합니다.

`document_daily_stats`는 테이블 생성 후 현재 문서 데이터 기준 초기 스냅샷을 멱등 반영합니다.

`front_content_view_event`는 공개 콘텐츠 조회를 문서·방문자·날짜별로 중복 제거합니다. 애플리케이션 배포 전에 테이블을 생성해야 하며 기존 `document.view_count` 값에는 영향을 주지 않습니다. 프론트 홈은 이 테이블의 최근 7일 이벤트를 집계해 공개·게시 완료 콘텐츠의 주간 인기 순위를 표시합니다.

`front_content_reaction`은 문서·방문자 조합을 유니크 키로 유지해 반복 요청을 중복 집계하지 않습니다. 반응 변경은 원자적 upsert로 처리되며 문서 삭제 시 애플리케이션 트랜잭션에서 관련 반응을 먼저 정리합니다.
반응 조회의 방문자 키는 URL이 아닌 `X-Content-Visitor-Key` 헤더로 전달해 access log와 브라우저 히스토리에 남지 않게 합니다.
관리자 반응 분석은 `updated_dtm` 인덱스를 사용해 7·14·30일 기간을 제한하고, 게시판별 도움 비율·일별 활동·반응 상위·개선 필요 콘텐츠를 고정 개수의 집계 쿼리로 조회합니다. 반응 변경 이력을 별도로 저장하지 않으므로 추이는 선택 이벤트 누계가 아니라 기간 내 각 방문자의 마지막 선택 상태를 의미합니다.

`admin_operation_task_content_source.sql`은 기존 운영 작업에 nullable 출처 컬럼을 추가합니다. 기존 행은 변경하지 않으며 `(source_type, source_id)` 유니크 키로 동일 콘텐츠 효과 분석에서 운영 작업이 중복 생성되는 것을 차단합니다. ALTER 스크립트이므로 환경별 마이그레이션 이력에서 한 번만 실행합니다.

배포 후 `/api/front/content?sort=POPULAR&size=4` 응답의 `sort`, `pageViewCount`, `pagePinnedCount` 필드를 확인합니다. 콘텐츠 아카이브의 조회순은 누적 `document.view_count`를 사용하며 고정 콘텐츠를 항상 먼저 노출합니다.

콘텐츠 상세 API의 `estimatedReadMinutes`, `characterCount`, `newerContent`, `olderContent`는 추가 응답 필드입니다. 이전·다음 콘텐츠는 동일 게시판의 공개·게시 완료 문서만 `(crt_dtm, no)` 순서로 조회하므로 기존 URL과 요청 계약은 변경되지 않습니다.

## 2. 릴리스 게이트

병합과 배포 전에 아래 명령을 실행합니다. 이 검증은 공백 오류, 관리자 JavaScript 단위 테스트, 전체 Gradle 테스트와 JAR 빌드를 포함합니다. 프론트 템플릿이 존재하지 않는 로컬 CSS·JavaScript를 참조하거나 캐시 버전(`?v=YYYYMMDD.N`)을 누락한 경우에도 `FrontStorefrontResourceTest`가 실패합니다.

```bash
./scripts/verify-release.sh
```

GitHub Actions의 `Release Verification`도 같은 명령을 pull request와 `main`·`master` push에서 실행합니다. 이 게이트가 통과하지 않은 커밋은 배포 후보로 취급하지 않습니다.

브라우저 회귀 검증까지 필요한 릴리스 후보는 MySQL이 준비된 환경에서 다음처럼 실행합니다. Playwright는 공유 상태 초기화 순서를 안정적으로 검증하기 위해 기본 1 워커로 Chrome 데스크톱과 Pixel 7 크기에서 인증, 장바구니·주문, 배송지, MY, 주문조회 등 주요 흐름과 가로 overflow를 확인합니다.

```bash
RUN_E2E=true ./scripts/verify-release.sh
```

## 3. 빌드

```bash
./gradlew clean test bootJar
```

실행 파일은 다음 경로에 생성됩니다.

- `Front/build/libs/Front-0.0.1-SNAPSHOT.jar`
- `admin/build/libs/admin-0.0.1-SNAPSHOT.jar`
- `batch/build/libs/batch-0.0.1-SNAPSHOT.jar`

`common`은 실행 애플리케이션이 아니라 각 모듈에 포함되는 라이브러리 JAR입니다.

## 4. 공통 환경변수

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://db-host:3306/new_toy?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export DB_USERNAME='grade_stock_app'
export DB_PASSWORD='replace-with-secret-manager-value'
export DB_MAX_POOL_SIZE=20
```

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 운영 프로파일에서 필수입니다. 비밀번호는 셸 파일이나 저장소에 기록하지 않고 배포 플랫폼의 secret 기능으로 주입합니다.

## 5. 최초 관리자

`admin_user` 테이블이 비어 있는 최초 배포에서만 다음 값을 지정합니다.

```bash
export ADMIN_BOOTSTRAP_LOGIN_ID='ops.master'
export ADMIN_BOOTSTRAP_PASSWORD='replace-with-at-least-12-characters'
export ADMIN_BOOTSTRAP_NAME='운영 총괄'
```

빈 DB에서 값이 누락되면 관리자 애플리케이션은 의도적으로 기동에 실패합니다. 계정이 생성된 다음 배포부터 bootstrap secret은 제거할 수 있습니다. 관리자 비밀번호는 PBKDF2로 저장되며 기존 평문 계정은 첫 로그인 시 자동 전환됩니다.

관리자 세션은 기본 30분이며 쿠키는 운영에서 `Secure`, `HttpOnly`, `SameSite=Strict`로 발급됩니다. 따라서 관리자 서비스는 반드시 HTTPS로 제공해야 합니다.

## 6. 실행

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

관리자는 콘텐츠 목록의 `문서 일일 통계` 영역과 `/api/admin/content/stats/daily`에서 최신 TOTAL·게시판별 스냅샷을 확인할 수 있습니다. 같은 화면의 `프론트 조회 분석` 영역과 `/api/admin/content/stats/views?boardType=NOTICE&days=7`에서는 실제 조회 이벤트의 7·14·30일 추이, 순 방문자, 상위 콘텐츠를 확인합니다. `/api/admin/content/stats/views/quality`는 전체·정상·고아 이벤트와 수집 기간을 제공하며 고아 이벤트가 있으면 화면에 `정리 필요`로 표시합니다.
같은 화면의 `독자 반응 분석`과 `/api/admin/content/stats/reactions?boardType=NOTICE&days=7`은 도움됨·개선 필요 반응을 집계합니다. `/api/admin/content/stats/reactions/export`는 동일한 게시판·기간 조건의 요약, 일별 추이, 반응 상위와 개선 필요 콘텐츠를 UTF-8 BOM CSV로 제공합니다.
`/api/admin/content/stats/reactions/quality`는 전체·정상·고아 반응과 수집 기간을 반환합니다. 고아 반응이 존재하면 `CLEANUP_REQUIRED`로 표시하지만 자동 삭제하지 않으며, 정리는 운영 데이터 확인 후 별도 절차로 수행해야 합니다.
`콘텐츠 효과 분석`과 `/api/admin/content/stats/performance?boardType=NOTICE&days=7`은 조회 상위와 반응 상위 후보를 문서 단위로 병합해 최대 10개의 조치 우선순위를 제공합니다. `반응 확보율`은 기간 조회수 대비 현재 반응 수를 비교한 방향성 운영 지표이며 방문별 전환율을 의미하지 않습니다. `/api/admin/content/stats/performance/export`는 같은 조건의 판단 근거와 점수를 UTF-8 BOM CSV로 제공합니다.
보완 필요 또는 반응 확보 필요 카드의 `작업 생성`은 `POST /api/admin/content/{id}/performance-task?boardType=NOTICE&days=7`을 호출합니다. 서버가 최신 분석 결과를 다시 검증하고 우선순위와 마감일을 계산하며, 문서 잠금과 출처 유니크 키로 중복 생성을 방지합니다. 연결된 작업 상세에서는 원본 콘텐츠로 복귀할 수 있습니다.
`POST /api/admin/content/stats/performance/tasks?boardType=NOTICE&days=7`은 화면에 표시된 최대 10개 우선순위 중 미연결 조치 대상을 일괄 작업화합니다. 분석 스냅샷은 한 번만 계산하고 문서를 번호 오름차순으로 잠근 뒤 기존 출처를 일괄 조회하므로 N+1 조회와 다중 요청의 교착 가능성을 줄입니다. 응답의 `createdCount`, `existingCount`, `skippedCount`로 신규·기존·삭제 경합 제외 결과를 확인합니다.
효과 분석의 `openTaskCount`, `overdueTaskCount`, `recoverableTaskCount`는 표시 중인 최대 10개 콘텐츠에 연결된 작업의 진행·기한·성과 회복 상태를 나타냅니다. 성과 회복은 현재 분석 상태가 `HEALTHY`이고 연결 작업이 완료 전일 때만 인정합니다. `POST /api/admin/content/stats/performance/tasks/resolve?boardType=NOTICE&days=7`은 회복 후보 작업을 번호 오름차순으로 잠근 뒤 출처와 최신 작업 상태를 재검증하여 `DONE`으로 변경합니다. 응답에서 완료·기존 완료·경합 제외 건수를 구분하며, 운영 스모크 테스트는 데이터 변경 방지를 위해 이 POST를 호출하지 않습니다.
`unassignedTaskCount`와 `assignmentRecommendations`는 회복 완료 후보를 제외한 미배정 진행 작업과 활성 관리자 3명의 현재 부하를 제공합니다. `POST /api/admin/content/stats/performance/tasks/assign?boardType=NOTICE&days=7`은 추천 관리자의 활성 상태를 일괄 재검증하고 작업을 번호순 잠근 뒤 `연체 → 진행중 → 전체 작업 → 관리자 번호` 순으로 최소 부하 담당자를 매 작업마다 다시 선택합니다. 응답의 `assignedCount`, `alreadyAssignedCount`, `skippedCount`로 배정·경합 결과를 구분합니다. 운영 스모크 테스트는 실제 담당자 변경 방지를 위해 이 POST를 호출하지 않습니다.
관리자 대시보드 응답의 `contentReactionSnapshot`은 최근 7일 도움 비율, 평가 콘텐츠, 데이터 품질과 최우선 개선 문서를 제공합니다. `/api/admin/content/{id}/reactions?days=30`은 문서별 전체 누계와 7·30·90일 최근 활동을 분리해 반환합니다.

조회 이벤트가 장기간 누적되는 운영 환경에서는 보존 배치를 활성화합니다. 기본값은 비활성이며, 활성화 시 매일 03:30에 오늘을 포함한 최근 180일을 유지하고 그 이전 이벤트를 단일 bulk delete로 정리합니다.

```bash
export BATCH_CONTENT_VIEW_RETENTION_ENABLED=true
export BATCH_CONTENT_VIEW_RETENTION_CRON='0 30 3 * * *'
export BATCH_CONTENT_VIEW_RETENTION_DAYS=180
```

`BATCH_CONTENT_VIEW_RETENTION_DAYS`는 실수로 최근 데이터를 대량 삭제하지 않도록 30일 이상 3650일 이하만 허용합니다. 삭제 성능은 `front_content_view_event.viewed_date` 인덱스를 사용합니다. 배치는 삭제된 문서를 참조하는 고아 이벤트를 먼저 정리한 뒤 기간 만료 데이터를 삭제하며, 로그의 `retentionStartDate`, `orphanDeleted`, `expiredDeleted`, `totalDeleted` 값으로 실행 결과를 확인합니다.

## 7. 배포 후 확인

각 애플리케이션은 다음 엔드포인트를 제공합니다.

- `/health/live`: 프로세스 생존 여부
- `/health/ready`: DB 연결과 `schema_migration`, 상품, 공유 요청 제한, 회원 활동 테이블을 포함한 요청 처리 준비 여부

```bash
FRONT_URL=https://service.example.com \
ADMIN_URL=https://admin.example.com \
BATCH_URL=http://batch.internal:9091 \
FRONT_DETAIL_PRODUCT_ID=12 \
FRONT_CONTENT_ID=1 \
ADMIN_SMOKE_LOGIN_ID=smoke-admin \
ADMIN_SMOKE_PASSWORD='replace-with-secret' \
./scripts/smoke-test.sh
```

`FRONT_DETAIL_PRODUCT_ID`와 `FRONT_CONTENT_ID`에는 운영 DB에 존재하는 대표 상품·콘텐츠 ID를 지정합니다. 프론트 smoke test는 메인, 신규 컬렉션, 여름 이벤트, 장바구니, 주문 조회, 비교, MY, 콘텐츠 아카이브 화면과 카탈로그·상품 상세·장바구니·콘텐츠 반응 조회 API, 미등록 상품의 `F002/404` 오류 계약을 확인합니다. 장바구니·주문·반응 데이터를 변경하는 요청은 호출하지 않습니다. 관리자 smoke test는 비로그인 화면 요청의 로그인 리다이렉트와 `/api/admin/**`의 `401`을 확인합니다. `ADMIN_SMOKE_LOGIN_ID`, `ADMIN_SMOKE_PASSWORD`를 제공하면 실제 로그인, 대시보드, 문서 일일 통계·프론트 조회·독자 반응·콘텐츠 효과 분석 API와 CSV 다운로드, 로그아웃 및 `Clear-Site-Data`까지 추가 검증하며 값은 로그에 출력하지 않습니다.

세 애플리케이션은 응답의 `X-Request-Id`를 공통 장애 추적 키로 사용합니다. 프록시가 안전한 요청 ID를 전달하면 그대로 유지하고, 없거나 형식이 잘못된 경우 애플리케이션이 새 ID를 생성합니다. 요청 헤더 한도는 기본 16KB이며 필요한 경우 `SERVER_MAX_HTTP_REQUEST_HEADER_SIZE`로 조정합니다.

관리자 세션 유효 시간은 `ADMIN_SESSION_TIMEOUT` 설정을 그대로 사용합니다. 로그아웃 시 관리자 서브도메인의 캐시, 쿠키, 브라우저 저장소를 제거하므로 운영 작업 데이터가 공용 브라우저에 남지 않습니다.

## 8. 운영 및 롤백

- load balancer는 `/health/ready`가 `200`인 인스턴스에만 트래픽을 전달합니다.
- 종료 시 Spring graceful shutdown이 진행되므로 프로세스 종료 유예 시간을 30초 이상 둡니다.
- 배포 실패 시 이전 JAR로 롤백하되, DB 변경이 포함됐다면 사전에 준비한 하위 호환 롤백 절차를 따릅니다.
- `/health/ready` 실패, 로그인 불가, 주요 목록 API의 반복적인 `5xx`가 발생하면 신규 버전 트래픽을 중단합니다.

## 9. 배포 판정

다음 순서를 모두 충족할 때에만 배포를 완료로 판정합니다.

1. 배포 전 DB 백업과 `./scripts/migrate-db.sh`의 성공 로그를 보관한다.
2. `./scripts/verify-release.sh`와 필요한 경우 `RUN_E2E=true` 검증이 통과한다.
3. 신규 인스턴스의 `/health/live`, `/health/ready`가 모두 `200`이 된 뒤에만 트래픽을 연결한다.
4. `./scripts/smoke-test.sh`가 프론트·관리자·배치에서 모두 성공한다.
5. 배포 후 오류율, 로그인 실패율, 주문 API `5xx`와 DB 커넥션 풀 포화를 최소 30분 관찰한다.

스테이징 URL, 운영 DB 접근 권한, TLS 인증서와 배포 플랫폼 권한은 저장소에 포함하지 않습니다. 실제 환경에서는 해당 secret을 배포 플랫폼에서 주입하고, 위 명령을 스테이징에서 먼저 실행한 뒤 운영에 동일한 절차를 적용합니다.
