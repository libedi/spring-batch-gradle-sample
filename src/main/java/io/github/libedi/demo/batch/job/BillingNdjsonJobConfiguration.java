package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.config.AppBatchProperties;
import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import io.github.libedi.demo.batch.mapper.customer.CustomerMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

@Configuration
public class BillingNdjsonJobConfiguration {

    @Bean
    public Job billingNdjsonJob(JobRepository jobRepository, Step billingNdjsonStep) {
        return new JobBuilder("billingNdjsonJob", jobRepository)
                .start(billingNdjsonStep)
                .build();
    }

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

    @Bean
    public BillingIdPagingReader billingIdPagingReader(
            BillingMapper billingMapper,
            AppBatchProperties appBatchProperties
    ) {
        return new BillingIdPagingReader(billingMapper, appBatchProperties.pageSize());
    }

    @Bean
    public ItemProcessor<Long, BillDataLine> billingLineProcessor(
            BillingMapper billingMapper,
            CustomerMapper customerMapper
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
            payload.put("dueDate", detail.dueDate());
            payload.put("customerName", customer.customerName());
            payload.put("email", customer.email());

            return new BillDataLine(header.id(), toJsonLine(payload));
        };
    }

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

    private String toJsonLine(Map<String, Object> payload) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(entry.getKey()).append('"').append(':');
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append('"').append(String.valueOf(value)).append('"');
            }
            first = false;
        }
        json.append("}\n");
        return json.toString();
    }
}
