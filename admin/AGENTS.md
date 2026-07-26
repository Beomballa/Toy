# AGENTS.md

## 적용 범위

`admin` 백엔드 작업에 우선 적용합니다.

---

## 백엔드 코드 원칙

JPA, QueryDSL은 단순 동작 구현보다 조회 구조, 확장성, 책임 분리를 고려하여 작성합니다.

import로 대체 가능한 FQCN(Fully Qualified Class Name)은 본문에서 직접 사용하지 않습니다.

헷갈릴 수 있는 조회 의도나 분기에는 짧고 명확한 주석만 허용합니다.

---

## Spring / Backend 규칙

기존 API 계약을 변경하지 않습니다.

다음 항목은 사용자 요청 없이는 변경하지 않습니다.

* URL
* Request Field
* Response Field
* DTO Field Name
* Entity Column Mapping
* DB Column Name

새로운 API 추가 전 기존 Mapping 중복 여부를 확인합니다.

Controller, Service, Repository 계층 구조는 기존 패턴을 우선 따릅니다.

@Transactional 은 명확한 이유 없이 추가하지 않습니다.

조회 구현 시 N+1 가능성을 검토합니다.

기존 통계, 쿠폰, 회원, 스탬프 기능의 조회 흐름과 응답 구조를 유지합니다.

---

## QueryDSL 규칙

DTO Projection 시 생성자 순서와 타입을 반드시 검증합니다.

가능하면 Tuple 반환보다 DTO 반환을 우선합니다.

Projection 변경 시 DTO 생성자와 QueryDSL 생성자 순서가 일치하는지 확인합니다.

기존 Repository 구현 패턴을 우선 참고합니다.

---

신규 기능은 가능하면 정상 케이스와 예외 케이스를 모두 작성합니다.
