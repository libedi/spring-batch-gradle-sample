package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.config.AppBatchProperties;
import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import io.github.libedi.demo.batch.mapper.customer.CustomerMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Strings;
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
 * Declares job/step/reader/processor/writer beans for NDJSON billing export.
 */
@Configuration
public class BillingNdjsonJobConfiguration {

    /**
     * Creates the NDJSON billing job.
     *
     * @param jobRepository batch job repository
     * @param billingNdjsonStep processing step
     * @return job definition
     */
    @Bean
    public Job billingNdjsonJob(JobRepository jobRepository, Step billingNdjsonStep) {
        return new JobBuilder("billingNdjsonJob", jobRepository)
                .start(billingNdjsonStep)
                .build();
    }

    /**
     * Creates the chunk-oriented step for billing NDJSON generation.
     *
     * @param jobRepository batch job repository
     * @param billTransactionManager transaction manager for bill datasource
     * @param billingIdPagingReader billing ID reader
     * @param billingLineProcessor item processor that joins bill/customer data
     * @param billDataWriter item writer that stores NDJSON
     * @param appBatchProperties batch runtime properties
     * @return step definition
     */
    @Bean
    public Step billingNdjsonStep(
            JobRepository jobRepository,
            @Qualifier("billTransactionManager") PlatformTransactionManager billTransactionManager,
            BillingIdPagingReader billingIdPagingReader,
            ItemProcessor<Long, BillDataLine> billingLineProcessor,
            ItemWriter<BillDataLine> billDataWriter,
            AppBatchProperties appBatchProperties
    ) {
        return new StepBuilder("billingNdjsonStep", jobRepository)
                .<Long, BillDataLine>chunk(appBatchProperties.chunkSize())
                .transactionManager(billTransactionManager)
                .reader(billingIdPagingReader)
                .processor(billingLineProcessor)
                .writer(billDataWriter)
                .build();
    }

    /**
     * Creates paging reader for target billing IDs.
     *
     * @param billingMapper bill mapper
     * @param appBatchProperties batch runtime properties
     * @return paging reader
     */
    @Bean
    public BillingIdPagingReader billingIdPagingReader(
            BillingMapper billingMapper,
            AppBatchProperties appBatchProperties
    ) {
        return new BillingIdPagingReader(billingMapper, appBatchProperties.pageSize());
    }

    /**
     * Creates processor that joins bill detail and customer rows into NDJSON line data.
     *
     * @param billingMapper bill mapper
     * @param customerMapper customer mapper
     * @return item processor
     */
    @Bean
    public ItemProcessor<Long, BillDataLine> billingLineProcessor(
            BillingMapper billingMapper,
            CustomerMapper customerMapper,
            ObjectMapper objectMapper
    ) {
        return billingId -> {
            BillingHeader header = billingMapper.findBillingHeader(billingId);
            BillingDetail detail = billingMapper.findBillingDetail(billingId);
            CustomerInfo customer = customerMapper.findCustomer(billingId);

            if (header == null || detail == null || customer == null) {
                return null;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("billingId", header.id());
            payload.put("billingNo", header.billingNo());
            payload.put("amount", detail.amount());
            payload.put("dueDate", Objects.toString(detail.dueDate(), ""));
            payload.put("customerName", customer.customerName());
            payload.put("email", customer.email());

            return new BillDataLine(header.id(), toJsonLine(payload, objectMapper));
        };
    }

    /**
     * Creates writer that stores NDJSON and marks processed billing rows.
     *
     * @param billDataMapper mapper for output table writes
     * @param billingMapper mapper for processed flag updates
     * @return item writer
     */
    @Bean
    public ItemWriter<BillDataLine> billDataWriter(
            BillDataMapper billDataMapper,
            BillingMapper billingMapper
    ) {
        return chunk -> {
            List<? extends BillDataLine> items = chunk.getItems();
            if (items.isEmpty()) {
                return;
            }

            billDataMapper.insertBatch(List.copyOf(items));
            billingMapper.markProcessed(items.stream().map(BillDataLine::billingId).toList());
        };
    }

    /**
     * Converts ordered payload entries into a single NDJSON line.
     *
     * @param payload ordered payload fields
     * @param objectMapper jackson object mapper
     * @return one JSON line with trailing line separator
     */
    private String toJsonLine(Map<String, Object> payload, ObjectMapper objectMapper) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (Strings.CS.endsWith(json, "\n")) {
                return json;
            }
            return json + "\n";
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize bill payload as NDJSON.", exception);
        }
    }
}
