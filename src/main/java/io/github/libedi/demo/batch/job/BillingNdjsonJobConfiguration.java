package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.config.AppBatchProperties;
import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.job.subtable.BillingDetailSubTableReader;
import io.github.libedi.demo.batch.job.subtable.CustomerSubTableReader;
import io.github.libedi.demo.batch.job.subtable.SubTableReader;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import io.github.libedi.demo.batch.mapper.customer.CustomerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
     * @param billingKeysetChunkReader Keyset 청구 ID 페이지 리더
     * @param billingJoinChunkProcessor 청크 단위 조합 Processor
     * @param billDataWriter NDJSON 저장/처리상태 갱신 Writer
     * @param appBatchProperties 배치 실행 속성
     * @return Step 정의
     */
    @Bean
    public Step billingNdjsonStep(
            JobRepository jobRepository,
            @Qualifier("billTransactionManager") PlatformTransactionManager billTransactionManager,
            BillingKeysetChunkReader billingKeysetChunkReader,
            ItemProcessor<List<Long>, BillingJoinChunk> billingJoinChunkProcessor,
            ItemWriter<BillingJoinChunk> billDataWriter,
            AppBatchProperties appBatchProperties
    ) {
        return new StepBuilder("billingNdjsonStep", jobRepository)
                .<List<Long>, BillingJoinChunk>chunk(appBatchProperties.chunkSize())
                .transactionManager(billTransactionManager)
                .reader(billingKeysetChunkReader)
                .processor(billingJoinChunkProcessor)
                .writer(billDataWriter)
                .build();
    }

    /**
     * 대상 청구 ID용 Keyset 페이징 리더를 생성합니다.
     *
     * @param billingMapper bill 매퍼
     * @param appBatchProperties 배치 실행 속성
     * @return 페이징 리더
     */
    @Bean
    public BillingKeysetChunkReader billingKeysetChunkReader(
            BillingMapper billingMapper,
            AppBatchProperties appBatchProperties
    ) {
        return new BillingKeysetChunkReader(
                billingMapper,
                appBatchProperties.pageSize(),
                appBatchProperties.maxId()
        );
    }

    /**
     * 청크 단위로 bill/customer 데이터를 조합하는 Processor를 생성합니다.
     *
     * @return ItemProcessor
     */
    @Bean
    public ItemProcessor<List<Long>, BillingJoinChunk> billingJoinChunkProcessor(
            BillingMapper billingMapper,
            SubTableReader<BillingDetail> billingDetailSubTableReader,
            SubTableReader<CustomerInfo> customerSubTableReader,
            BillDataMapper billDataMapper,
            ObjectMapper objectMapper
    ) {
        return new BillingJoinChunkProcessor(
                billingMapper,
                billingDetailSubTableReader,
                customerSubTableReader,
                billDataMapper,
                objectMapper
        );
    }

    /**
     * billing_detail 조회용 서브리더를 생성합니다.
     *
     * @param billingMapper billing 조회 매퍼
     * @return SubTableReader 구현체
     */
    @Bean
    public SubTableReader<BillingDetail> billingDetailSubTableReader(BillingMapper billingMapper) {
        return new BillingDetailSubTableReader(billingMapper);
    }

    /**
     * customer 조회용 서브리더를 생성합니다.
     *
     * @param customerMapper customer 조회 매퍼
     * @return SubTableReader 구현체
     */
    @Bean
    public SubTableReader<CustomerInfo> customerSubTableReader(CustomerMapper customerMapper) {
        return new CustomerSubTableReader(customerMapper);
    }

    /**
     * NDJSON 저장과 billing 처리 상태 갱신을 수행하는 Writer를 생성합니다.
     *
     * @param billDataMapper 출력 테이블 저장 매퍼
     * @param billingMapper 처리 플래그 갱신 매퍼
     * @return ItemWriter
     */
    @Bean
    public ItemWriter<BillingJoinChunk> billDataWriter(
            BillDataMapper billDataMapper,
            BillingMapper billingMapper
    ) {
        return new BillingNdjsonItemWriter(billDataMapper, billingMapper);
    }
}


