package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.config.AppBatchProperties;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import io.github.libedi.demo.batch.mapper.customer.CustomerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * NDJSON 청구 내보내기용 Job/Step/Reader/Processor/Writer 빈을 선언합니다.
 */
@Configuration
public class BillingNdjsonJobConfiguration {

    /**
     * NDJSON 청구 Job을 생성합니다.
     *
     * @param jobRepository 배치 Job 저장소
     * @param billingNdjsonStep 처리 Step
     * @return Job 정의
     */
    @Bean
    public Job billingNdjsonJob(JobRepository jobRepository, Step billingNdjsonStep) {
        return new JobBuilder("billingNdjsonJob", jobRepository)
                .start(billingNdjsonStep)
                .build();
    }

    /**
     * billing NDJSON 생성을 위한 청크 기반 Step을 생성합니다.
     *
     * @param jobRepository 배치 Job 저장소
     * @param billTransactionManager bill 데이터소스 트랜잭션 매니저
     * @param billingIdPagingReader 청구 ID 리더
     * @param billingLineProcessor 청구 ID를 정규화하는 ItemProcessor
     * @param billDataWriter 배치 조인 및 NDJSON 저장을 수행하는 ItemWriter
     * @param appBatchProperties 배치 실행 속성
     * @return Step 정의
     */
    @Bean
    public Step billingNdjsonStep(
            JobRepository jobRepository,
            @Qualifier("billTransactionManager") PlatformTransactionManager billTransactionManager,
            BillingIdPagingReader billingIdPagingReader,
            ItemProcessor<Long, Long> billingLineProcessor,
            ItemWriter<Long> billDataWriter,
            AppBatchProperties appBatchProperties
    ) {
        return new StepBuilder("billingNdjsonStep", jobRepository)
                .<Long, Long>chunk(appBatchProperties.chunkSize())
                .transactionManager(billTransactionManager)
                .reader(billingIdPagingReader)
                .processor(billingLineProcessor)
                .writer(billDataWriter)
                .build();
    }

    /**
     * 대상 청구 ID용 페이징 리더를 생성합니다.
     *
     * @param billingMapper bill 매퍼
     * @param appBatchProperties 배치 실행 속성
     * @return 페이징 리더
     */
    @Bean
    public BillingIdPagingReader billingIdPagingReader(
            BillingMapper billingMapper,
            AppBatchProperties appBatchProperties
    ) {
        return new BillingIdPagingReader(billingMapper, appBatchProperties.pageSize());
    }

    /**
     * bill 상세와 customer 행을 조인해 NDJSON 라인 데이터를 만드는 Processor를 생성합니다.
     *
     * @return ItemProcessor
     */
    @Bean
    public ItemProcessor<Long, Long> billingLineProcessor() {
        return new BillingLineItemProcessor();
    }

    /**
     * NDJSON 저장과 billing 처리 상태 갱신을 수행하는 Writer를 생성합니다.
     *
     * @param billDataMapper 출력 테이블 저장 매퍼
     * @param billingMapper 처리 플래그 갱신 매퍼
     * @return ItemWriter
     */
    @Bean
    public ItemWriter<Long> billDataWriter(
            BillDataMapper billDataMapper,
            BillingMapper billingMapper,
            CustomerMapper customerMapper,
            ObjectMapper objectMapper
    ) {
        return new BillingNdjsonItemWriter(billDataMapper, billingMapper, customerMapper, objectMapper);
    }
}


