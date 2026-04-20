# spring-batch-gradle-sample

멀티 DB 커넥션을 사용하는 대용량 배치 샘플 프로젝트입니다.  
`billing` + `billing_detail` + `customer` 데이터를 조합해 `bill_data` 테이블에 NDJSON 라인을 적재합니다.

## 핵심 목표
- 2개 업무 DB(`bill`, `customer`) + 1개 배치 메타 DB(`batch`) 분리
- `billing` 대상 건 조회 후 상세/고객 정보를 조합
- 결과를 `bill_data.payload_ndjson`에 NDJSON 포맷으로 저장
- 대량(약 1,000만 건) 처리를 고려한 Chunk/Page 기반 배치 처리

## 기술 스택
- Java 25
- Spring Boot 4.0.5
- Spring Batch 6.x
- MyBatis (mybatis-spring-boot-starter 4.0.1)
- H2
- Lombok
- Gradle Wrapper

## 데이터 모델
- bill 스키마
- `billing` (메인)
- `billing_detail` (서브)
- `bill_data` (결과 저장)
- customer 스키마
- `customer` (`billing.id`와 1:1 연관)

관계:
- `billing` : `billing_detail` = 1:1
- `billing` : `customer` = 1:1

## 프로젝트 구조
```text
spring-batch-gradle-sample
├─ src/main/java/io/github/libedi/demo
│  ├─ DemoApplication.java
│  └─ batch
│     ├─ config
│     │  ├─ AppBatchProperties.java
│     │  ├─ DataSourceConfiguration.java
│     │  └─ MybatisConfiguration.java
│     ├─ domain
│     │  ├─ BillingHeader.java
│     │  ├─ BillingDetail.java
│     │  ├─ CustomerInfo.java
│     │  └─ BillDataLine.java
│     ├─ mapper
│     │  ├─ bill
│     │  │  ├─ BillingMapper.java
│     │  │  └─ BillDataMapper.java
│     │  └─ customer
│     │     └─ CustomerMapper.java
│     └─ job
│        ├─ BillingIdPagingReader.java
│        └─ BillingNdjsonJobConfiguration.java
├─ src/main/resources
│  ├─ application.yml
│  ├─ config/application.yml
│  └─ schema
│     ├─ bill.sql
│     └─ customer.sql
├─ src/test
│  ├─ java/io/github/libedi/demo/batch/BillingNdjsonJobIntegrationTest.java
│  └─ resources
│     ├─ application-test.yml
│     └─ schema
│        ├─ bill.sql
│        └─ customer.sql
└─ AGENTS.md
```

## 구현 방식
### 1) 멀티 데이터소스 분리
- `batchDataSource`(Primary): JobRepository/배치 메타데이터 전용
- `billDataSource`: `billing`, `billing_detail`, `bill_data` 읽기/쓰기
- `customerDataSource`: `customer` 읽기 전용
- 각 데이터소스별 `PlatformTransactionManager`를 별도 구성해 트랜잭션 경계를 명확히 분리

### 2) MyBatis 세션 템플릿 분리
- bill/customer 각각 독립 `SqlSessionFactory`, `SqlSessionTemplate` 구성
- `@MapperScan`을 패키지 기준으로 분리해 매퍼가 올바른 DB 커넥션을 사용하도록 강제

### 3) 배치 Step 설계 (Chunk + Paging)
- Reader: `BillingIdPagingReader`
- `billing`에서 `processed = FALSE` 대상 ID를 페이지 단위 조회
- Processor: ID 기준으로 `billing_header + billing_detail + customer` 조합
- 조합 불가 데이터는 `null` 반환으로 필터링
- Writer: NDJSON 문자열을 `bill_data`에 일괄 insert
- Writer: 처리 완료된 `billing` ID를 `processed = TRUE`로 일괄 업데이트
- Step은 `chunk(size).transactionManager(...)` 형태로 구성해 최신 방식 사용

### 4) NDJSON 생성 규칙
- 한 레코드당 JSON 라인 1개를 생성하고 줄바꿈(`\n`)으로 종료
- 필드 순서를 고정해(LinkedHashMap 기반) 테스트/검증 시 일관된 결과 확보

### 5) 테스트 구현 방식
- `@SqlGroup` + datasource별 `@SqlConfig`로 bill/customer 테스트 데이터를 분리 주입
- Step 슬라이스 테스트: 실제 `billingNdjsonStep`을 단일 Step Job(`billingNdjsonStepSliceJob`)으로 감싸 검증
- Job 통합 테스트: 실제 `billingNdjsonJob` 전체 실행 후 결과 데이터/상태 검증
- 실행 진입점은 `JobOperator.start(Job, JobParameters)` 사용 (deprecated 방식 제외)

## 배치 처리 흐름
1. `billing`에서 미처리 대상 ID 페이지 조회
2. 각 ID별로 `billing_detail`, `customer`를 조회해 조합
3. 조합 결과를 NDJSON 문자열로 생성
4. `bill_data`에 배치 insert
5. 처리된 `billing` 건 `processed = true`로 업데이트

## 설정
주요 설정 파일:
- `src/main/resources/application.yml`
- `src/main/resources/config/application.yml`

원칙:
- 설정은 `application.yml` 중심
- Java Config는 필요한 경우에만 사용
- deprecated API 사용 금지

## 빌드/실행/테스트
프로젝트 루트에서 실행:

```bash
./gradlew clean build
```

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew clean build
.\gradlew test
```

## 테스트 전략
- Step 슬라이스 테스트 + Job 통합 테스트를 모두 유지
- 테스트 데이터 준비는 `@Sql` 기반
- 테스트용 DDL/DML 위치: `src/test/resources/schema/bill.sql`, `src/test/resources/schema/customer.sql`

예시 통합 테스트:
- `src/test/java/io/github/libedi/demo/batch/BillingNdjsonJobIntegrationTest.java`

## 품질 기준
- TDD 기반으로 기능 개발
- 테스트 통과 코드만 커밋
- 동일 테스트 5회 연속 실패 시 원인/대안 정리 후 재진행
- 중복 코드는 리팩토링으로 공통화

## 참고
- 기본 H2 기반 샘플 구성입니다.
- 실제 운영에서는 DB 연결 정보/스키마/성능 파라미터를 환경별로 분리하세요.

