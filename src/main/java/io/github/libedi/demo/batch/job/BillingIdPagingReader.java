package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

public class BillingIdPagingReader implements ItemStreamReader<Long> {

    private static final String LAST_ID_KEY = "billing.lastId";

    private final BillingMapper billingMapper;
    private final int pageSize;
    private final Queue<Long> buffer = new ArrayDeque<>();

    private long lastId;

    public BillingIdPagingReader(BillingMapper billingMapper, int pageSize) {
        this.billingMapper = billingMapper;
        this.pageSize = pageSize;
    }

    @Override
    public Long read() {
        if (buffer.isEmpty()) {
            List<Long> billingIds = billingMapper.findTargetBillingIds(lastId, pageSize);
            if (billingIds.isEmpty()) {
                return null;
            }
            buffer.addAll(billingIds);
            lastId = billingIds.getLast();
        }
        return buffer.poll();
    }

    @Override
    public void open(ExecutionContext executionContext) {
        lastId = executionContext.getLong(LAST_ID_KEY, 0L);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_ID_KEY, lastId);
    }

    @Override
    public void close() {
        buffer.clear();
    }
}
