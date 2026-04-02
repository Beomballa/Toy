# 🧸 Grade Stock Admin Project

> **Grade Stock**은 멀티 모듈 기반의 상품 관리 및 커머스 운영 시스템입니다.  
> 본 프로젝트는 확장성 있는 백엔드 설계와 모던한 UI/UX를 지향합니다.

---

## 🚀 Technology Stack

### Backend
- **Java 17** (Record pattern 활용)
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Querydsl 5.0** (동적 쿼리 및 검색 최적화)
- **Lombok** (Entity 및 내부 로직 간소화)
- **MySQL**

### Frontend
- **Thymeleaf** (Server-side Rendering)
- **Vanilla JS** (ES6+, Promise 기반 비동기 처리)
- **Bootstrap 5.3** & **Font Awesome 6**
- **CSS3** (Custom Modal 및 Animation)

---

## 🏗 Project Structure (Multi-Module)

프로젝트는 관심사의 분리(SoC)를 위해 **멀티 모듈** 구조로 설계되었습니다.

- **`common`**: 전사 공통 모듈
  - 핵심 도메인 엔티티 (`Product`, `Category`, `Brand` 등)
  - 공통 리포지토리 및 코어 비즈니스 서비스
  - 공용 유틸리티 클래스
- **`admin`**: 관리자 운영 시스템
  - 관리자 전용 REST API 및 View 컨트롤러
  - 화면 전용 DTO (Record 사용)
  - Thymeleaf 기반 운영 UI 및 전용 스크립트
- **`batch`**: 자동화 처리 시스템
  - 정기적인 데이터 갱신 및 상태 변경 스케줄러

---

## ✨ Key Features & Implementation

### 1. Record 기반의 Immutable DTO
- API 응답 구조에 Java **Record**를 전적으로 도입하여 데이터 불변성(Immutability)을 확보하고 보일러플레이트 코드를 최소화했습니다.
- 정적 팩토리 메서드(`from`, `of`)를 사용하여 엔티티에서 DTO로의 변환 로직을 캡슐화했습니다.

### 2. Promise 기반 커스텀 알림 시스템
- 브라우저 기본 `alert/confirm` 대신, `async/await`로 제어 가능한 **Promise 기반 커스텀 모달**을 구현했습니다.
- `CommonJS.confirm()` 호출 한 번으로 사용자의 확인 여부를 `boolean`으로 리턴받아 비즈니스 로직을 직관적으로 작성할 수 있습니다.

### 3. Querydsl 기반의 고도화된 검색
- `CustomProductRepository`를 통해 상품명, 브랜드, 카테고리, 상태값 등 다양한 필터 조건을 동적으로 처리합니다.
- 복잡한 조인 쿼리에서도 성능 최적화와 타입 안정성을 보장합니다.

### 4. 도메인 주도 설계 (DDD) 지향
- 엔티티 내부에 비즈니스 메서드(`changeStatus`, `updateBasicInfo` 등)를 배치하여 객체 스스로 상태를 제어하는 응집도 높은 도메인 모델을 구축했습니다.

---

## 🛠 Conventions

- **Branch Strategy:** Git-Flow 패턴 지향
- **Commit Message:** `Feat:`, `Fix:`, `Refactor:`, `Docs:` 등의 접두어 사용
- **API Design:** RESTful API 원칙 준수 (GET: 조회, POST: 등록, PATCH: 상태 변경/수정)
- **Frontend:** 전역 객체(`ProductList`, `ProductUpdate` 등)를 활용한 스크립트 캡슐화

---

## 📸 Screen Shots (Preview)
- 상품 목록 조회 및 필터링
- 상품 등록 및 실시간 미리보기
- 상품 상세 정보 및 옵션 관리
- 상품 정보 수정 및 논리 삭제 (Soft Delete)
