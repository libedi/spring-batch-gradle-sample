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

## Operational Review Notes
- 운영 기준으로 현재 processor는 N+1 조회 구조로 간주하며, `billing` 1건당 `billing_header`, `billing_detail`, `customer` 재조회 패턴은 지양
- 대용량(약 1,000만 건) 처리 시 DB round-trip 최소화를 위해 chunk/page 단위 일괄 조회(배치 조회) 전략을 우선 적용
- 멀티 DB 사용 시 트랜잭션 원자성 한계를 명시하고, 원칙적으로 `한 DB만 쓰기` + `나머지 DB는 읽기 전용` 정책을 유지
- 향후 읽기 전용 DB에 쓰기 요구가 생기면 현재 구조로는 분산 트랜잭션 원자성 보장이 어렵다는 점을 설계 문서와 코드에 명확히 기록
- 재시작 안전성은 reader 상태 저장만으로 충분하다고 가정하지 않으며, writer의 insert/update 순서에서 발생 가능한 중간 장애 상태를 반드시 고려
- `bill_data.billing_id` UNIQUE 제약은 중복 방지 수단으로만 사용하고, 운영 복구 전략으로는 upsert, skip/retry, 상태 전이 컬럼 분리 중 최소 1개 이상 적용
- 대량 처리 Job은 단일 Step/단일 스레드 전제를 기본값으로 고정하지 않고, partitioning / multi-threaded step / remote chunking 확장 경로를 사전 설계
- `page-size`, `chunk-size` 기본값(예: 1000)은 시작점으로만 사용하며, 실환경 성능 측정 후 튜닝 결과를 설정으로 외부화
- 프로젝트가 확장 단계에 진입하면 단일 모듈 구조를 유지하지 않고, 최소 `job`, `domain`, `infra-mybatis`, `boot` 경계로 멀티 모듈 분리를 검토
- `BillingNdjsonJobConfiguration`에 Reader/Processor/Writer/Step/Job 구성이 과도하게 집중되지 않도록 컴포넌트 분리 원칙을 적용
- Processor, Writer는 별도 클래스로 분리하고 단위 테스트 가능 구조를 우선하여 변경 용이성과 응집도를 관리

## Large-Scale Batch Architecture Notes
- 목표 처리량은 최대 2천만 건 규모를 기준으로 설계하고, Kubernetes pod 기반 병렬 처리 및 재시작 안정성을 우선
- 조회 전략은 Cursor/Offset 기반 대신 Keyset Pagination(`id > :lastId`)을 기본값으로 채택
- Keyset 조회는 항상 `ORDER BY id ASC` + `LIMIT`를 포함하고, 가능하면 상한 조건(`id <= :maxId`)을 함께 사용
- 대용량 구간에서 Offset Paging(`OFFSET`)은 성능 저하가 커서 운영 경로에서 사용하지 않음
- 장시간 커넥션 점유를 유발하는 Cursor Reader는 운영 기본 경로에서 사용하지 않음
- 병렬 처리는 `Range Partition` 중심으로 설계하고, 파티션 단위 상태 추적을 위한 별도 계획 테이블(`partition_plan`) 도입을 권장
- `partition_plan`은 실행 키(`job_execution_key`) 기준 append-only로 관리해 재시작/재처리 추적 가능성을 확보
- Worker는 파티션 범위(`min_id ~ max_id`)를 기준으로 독립 실행하고 `last_processed_id`/`status`를 갱신
- Reader 책임은 대상 key 조회, Processor 책임은 배치 조합/가공, Writer 책임은 저장/상태 갱신으로 분리
- Writer는 멱등성을 고려해 중복 방지(UNIQUE, upsert, 선조회 필터링 중 하나 이상)를 반드시 포함
- 장애 복구는 `ExecutionContext(lastId)`와 파티션 상태 테이블을 함께 사용해 중복/누락 리스크를 줄임
- 인덱스는 기본적으로 `PRIMARY KEY(id)`를 전제하고, 추가 조건이 있으면 `(status, id)` 복합 인덱스를 검토

## Keyset Reader Rules
- Keyset Reader는 `lastId`를 `ExecutionContext`에 저장/복원해야 하며 재시작 시 해당 값부터 이어서 처리
- Keyset Reader는 페이지 단위 조회를 내부적으로 수행하되, Step 처리 단위(아이템/페이지)는 Job 설계 의도에 맞춰 명시적으로 선택
- Keyset Reader 도입 시 통합 테스트 외에 Reader 단위 테스트(페이지 진행, 재시작, 상태 저장)를 반드시 추가

## Commit Strategy
- 커밋 메시지는 Angular 방식을 사용하여 메시지 앞에 커밋 성격을 알려주는 prefix를 사용할 것
- 반드시 테스트를 통과한 코드만 커밋을 수행

## Development Cycle
- 기능 개발 완료 후(테스트 통과 포함) `커밋 -> 푸쉬`를 완료하면, 즉시 운영 관점 개선/리팩토링 사이클을 시작
- 운영 관점 개선/리팩토링 대상은 현재 코드와 방금 반영된 기능 전체를 포함
- 개선/리팩토링 이후 반드시 테스트를 다시 수행하고, 필요 시 테스트 케이스를 추가
- 재검증이 끝난 결과는 `커밋 -> 푸쉬`까지 완료
- 위 사이클(기능 개발 완료 -> 커밋/푸쉬 -> 운영 관점 개선/리팩토링 -> 테스트(필요 시 추가) -> 커밋/푸쉬)을 반복 수행
- 본 문서 기준 작업에서는 사용자의 별도 재확인 없이 테스트 통과 직후 `커밋 -> 푸쉬`를 자동 수행
