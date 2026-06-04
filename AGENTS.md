# Toy Automation Convention

## 공통 원칙
1. `Toy` 프로젝트에서는 자동 커밋/자동 푸시를 하지 않는다.
2. 기능 단위로 작업하며, 기존 변경사항이 있는 영역은 충돌 여부를 먼저 확인한다.
3. JPA/QueryDSL은 단순 동작 구현이 아니라 조회 구조, 필터 확장성, 책임 분리를 고려해 작성한다.
4. 가독성이 떨어질 수 있는 분기나 조회 의도에는 짧은 주석을 허용한다.
5. 기능 단위 완료 후에는 테스트 코드를 함께 보완한다.

## 코드 스타일
1. import로 대체 가능한 FQCN(fully qualified class name)은 본문에서 직접 쓰지 않는다.
2. 예시:
   `private static final QAdminOperationTaskComment keywordComment = new QAdminOperationTaskComment("keywordComment");`
   처럼 import 후 짧은 타입명으로 통일한다.
3. 아래 경우도 동일하게 정리한다.
   - `new com.section...`
   - `Page<com.section...>`
   - `assertEquals(com.section...`
   - `ArgumentCaptor.forClass(com.section....class)`
   - enum, DTO, request/response, Querydsl Q 타입 참조
4. 예외:
   - JPQL 문자열 내부의 생성자 표현식 `SELECT new com.section...`
   - Spring 설정 어노테이션의 패키지 문자열
   - import/package 선언 자체

## 자동화 작업 시 보고
1. 어떤 작업을 했는지와 이전 대비 달라진 점을 함께 정리한다.
2. 운영 관점, 성능 관점, 유지보수 관점에서 실용적인 영향이 있으면 같이 설명한다.
3. 테스트를 실행하지 못한 경우에는 이유를 명확히 남긴다.
