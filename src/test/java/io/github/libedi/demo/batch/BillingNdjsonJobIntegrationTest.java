package io.github.libedi.demo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlGroup;

/**
 * Step 슬라이스와 전체 Job 실행 경로를 검증하는 통합 테스트입니다.
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@ActiveProfiles("test")
@SqlGroup({
        @Sql(
                scripts = "/schema/bill.sql",
                config = @SqlConfig(
                        dataSource = "billDataSource",
                        transactionManager = "billTransactionManager"
                ),
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
        ),
        @Sql(
                scripts = "/schema/customer.sql",
                config = @SqlConfig(
                        dataSource = "customerDataSource",
                        transactionManager = "customerTransactionManager"
                ),
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
        )
})
class BillingNdjsonJobIntegrationTest {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    @Qualifier("billingNdjsonStepSliceJob")
    private Job billingNdjsonStepSliceJob;

    @Autowired
    @Qualifier("billingNdjsonJob")
    private Job billingNdjsonJob;

    @Autowired
    private BillingMapper billingMapper;

    @Autowired
    private BillDataMapper billDataMapper;

    /**
     * Step 슬라이스 실행이 조인 가능한 데이터만 처리하는지 검증합니다.
     *
     * @throws Exception Job 실행 실패 시
     */
    @Test
    void billingNdjsonStepProcessesJoinableRows() throws Exception {
        JobExecution execution = jobOperator.start(billingNdjsonStepSliceJob, testJobParameters());

        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(billDataMapper.countBillData()).isEqualTo(2);
        assertThat(billingMapper.countProcessed()).isEqualTo(2);
    }

    /**
     * 전체 Job 실행이 기대한 NDJSON 데이터를 기록하는지 검증합니다.
     *
     * @throws Exception Job 실행 실패 시
     */
    @Test
    void billingNdjsonJobCompletesAndWritesNdjson() throws Exception {
        JobExecution execution = jobOperator.start(billingNdjsonJob, testJobParameters());

        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(billDataMapper.countBillData()).isEqualTo(2);
        assertThat(billingMapper.countProcessed()).isEqualTo(2);

        String payload = billDataMapper.findPayloadByBillingId(1L);
        assertThat(payload).contains("\"billingNo\":\"BILL-0001\"");
        assertThat(payload).contains("\"customerName\":\"Alice Kim\"");
    }

    /**
     * 재시작 상황에서 일부 NDJSON가 이미 저장된 경우에도 잡이 정상 완료되는지 검증합니다.
     *
     * @throws Exception Job 실행 실패 시
     */
    @Test
    void billingNdjsonJobCompletesWhenBillDataAlreadyExists() throws Exception {
        billDataMapper.insertBatch(List.of(new BillDataLine(1L, "{\"billingId\":1}\n")));

        JobExecution execution = jobOperator.start(billingNdjsonJob, testJobParameters());

        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(billDataMapper.countBillData()).isEqualTo(2);
        assertThat(billingMapper.countProcessed()).isEqualTo(2);
    }

    /**
     * 단일 Step을 독립 Job으로 실행하기 위한 테스트 전용 설정입니다.
     */
    @TestConfiguration
    static class StepSliceTestConfiguration {

        /**
         * 운영 Step을 단일 Step Job으로 감싸 슬라이스 테스트를 수행합니다.
         *
         * @param jobRepository Job 저장소
         * @param billingNdjsonStep 운영 Step 빈
         * @return 단일 Step 테스트 Job
         */
        @Bean(name = "billingNdjsonStepSliceJob")
        Job billingNdjsonStepSliceJob(
                JobRepository jobRepository,
                @Qualifier("billingNdjsonStep") Step billingNdjsonStep
        ) {
            return new JobBuilder("billingNdjsonStepSliceJob", jobRepository)
                    .start(billingNdjsonStep)
                    .build();
        }
    }

    /**
     * 테스트 Job 실행용 고유 파라미터를 생성합니다.
     *
     * @return 고유 타임스탬프를 포함한 Job 파라미터
     */
    private org.springframework.batch.core.job.parameters.JobParameters testJobParameters() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}


