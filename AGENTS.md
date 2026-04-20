# AGENTS.md

## Project Overview
이 프로젝트는 멀티 DB 커넥션을 활용한 대용량 배치 애플리케이션이다.
목표는 청구 데이터와 고객 데이터를 조합해 청구서 안내 데이터를 생성하는 것이다.

## Environment
- 프로젝트 루트에서 `.gradlew clean build`로 빌드
- 테스트는 프로젝트 루트에서 `.gradlew test`로 수행
- Java 25 + Spring Boot + Spring Batch + MyBatis + Lombok + H2
- DB는 2개의 멀티 커넥션을 유지 (bill 스키마, customer 스키마)
- bill 스키마는 조회/수정/등록 수행, customer 스키마는 조회만 수행
- bill 스키마에는 메인 테이블 `billing`, 서브 테이블 `billing_detail`
- customer 스키마는 `billing` 테이블 PK를 갖는 연관 정보 테이블
- `billing`과 `billing_detail`은 1:1 관계, `billing`과 `customer`도 1:1 관계
- JobRepository는 별도의 DB 스키마로 구성하며, 해당 스키마를 Primary로 유지
- MyBatis는 billing/customer 스키마 각각에 대한 커넥션 생성
- 대상 건수는 대략 1000만 건 규모
- 처리 흐름
  - `billing` 테이블에서 대상건 조회
  - `billing_detail`, `customer` 테이블 데이터 조합
  - 결과 데이터를 `bill_data` 테이블에 ndjson 형태로 insert

## Code Style
- Java 25 및 Spring Boot 4 이상 최신 기능을 활용
- 설정은 `application.yml`에 두고, 필요한 경우에만 Java Config 사용
- 설정 파일은 `resources/config/` 디렉토리에 위치
- `application.yml`에는 오직 spring 설정만 위치
- 설정 파일은 기능에 맞게 분리하여 `application.yml`의 `spring.config.import` 속성을 이용해 조합할 것
- 기본값 설정은 명시적으로 하지 않음
- deprecated 클래스/메서드/기능은 사용하지 않고 대체된 최신 방식 사용
- `@Value`는 사용하지 말고, `@ConfigurationProperties`를 활용할 것
- 최소한 클래스와 메서드에는 JavaDoc 주석을 작성할 것
- Apache Commons Lang3 라이브러리를 적극 활용할 것

## Testing & Quality
- 반드시 TDD로 개발하고, 기능은 해당 단위 테스트 코드가 정상 수행되어야 완료
- 하나의 Step은 기능별 슬라이스 테스트가 정상 수행되어야 완료
- 하나의 Job은 통합 테스트를 작성하여 정상 수행되어야 완료
- 동일한 테스트 코드가 5번 실패하면 반드시 작업을 멈추고 실패 이유와 해결책을 제시
- 테스트 수행을 위한 DDL/DML 쿼리는 `resources/schema/{{도메인}}.sql`로 생성하고 `@Sql` 활용
- 테스트 코드는 AssertJ를 기본으로, 필요한 경우 Mockito도 활용할 것

## Refactoring
- 중복 코드는 리팩토링을 통해 공통화
- 설정 리팩토링 시에도 `application.yml`을 최대한 활용

## Commit Strategy
- 커밋 메시지는 Angular 방식을 사용하여 메시지 앞에 커밋 성격을 알려주는 prefix를 사용할 것
- 반드시 테스트를 통과한 코드만 커밋을 수행