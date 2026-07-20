# Grade Stock

상품 탐색 프론트, 운영 관리자, 배치 애플리케이션으로 구성된 Spring Boot 멀티 모듈 프로젝트입니다.

## 구성

| 모듈 | 역할 | 기본 포트 |
| --- | --- | --- |
| `Front` | 고객용 상품 탐색 및 상세 화면 | `8080` |
| `admin` | 상품, 주문, 콘텐츠, 회원, 운영 설정 관리 | `9090` |
| `batch` | 운영 배치 실행기 | `9091` |
| `common` | 공통 엔티티, QueryDSL 저장소, 공통 서비스 | 라이브러리 |

## 기술 스택

- Java 21
- Spring Boot 3.5.4
- Spring Data JPA, OpenFeign QueryDSL 6.11
- Thymeleaf, Vanilla JavaScript
- MySQL

## 로컬 실행

MySQL의 `new_toy` 데이터베이스를 준비한 뒤 환경에 맞게 접속 정보를 지정합니다.

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/new_toy?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export DB_USERNAME='root'
export DB_PASSWORD='your-password'

./gradlew :Front:bootRun
./gradlew :admin:bootRun
```

로컬 프로파일은 데이터가 없는 테이블에 화면 확인용 데이터를 생성합니다. 운영 프로파일에서는 로컬 시더가 동작하지 않습니다.

## 검증

```bash
./gradlew test bootJar
```

배포 후 기본 포트로 세 애플리케이션을 실행했다면 다음 명령으로 헬스체크와 관리자 접근 통제를 확인할 수 있습니다.

```bash
./scripts/smoke-test.sh
```

운영 환경변수와 최초 관리자 생성, 실행 순서, 롤백 기준은 [배포 가이드](docs/DEPLOYMENT.md)를 확인합니다.
