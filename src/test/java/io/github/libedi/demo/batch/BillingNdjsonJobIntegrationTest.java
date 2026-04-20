package io.github.libedi.demo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
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
 * Integration tests for step-slice and full job execution paths.
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
     * Verifies step-slice execution processes joinable rows only.
     *
     * @throws Exception when job execution fails
     */
    @Test
    void billingNdjsonStepProcessesJoinableRows() throws Exception {
        JobExecution execution = jobOperator.start(billingNdjsonStepSliceJob, testJobParameters());

        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(billDataMapper.countBillData()).isEqualTo(2);
        assertThat(billingMapper.countProcessed()).isEqualTo(2);
    }

    /**
     * Verifies full job execution writes expected NDJSON content.
     *
     * @throws Exception when job execution fails
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
     * Test-only job configuration for running a single step as a standalone job.
     */
    @TestConfiguration
    static class StepSliceTestConfiguration {

        /**
         * Wraps production step into a single-step job for slice testing.
         *
         * @param jobRepository job repository
         * @param billingNdjsonStep production step bean
         * @return single-step test job
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
     * Creates unique parameters for test job execution.
     *
     * @return job parameters with unique timestamp
     */
    private org.springframework.batch.core.job.parameters.JobParameters testJobParameters() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}
