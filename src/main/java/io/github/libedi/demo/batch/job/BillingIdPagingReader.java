package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * Item reader that pages billing IDs from the bill datasource.
 */
public class BillingIdPagingReader implements ItemStreamReader<Long> {

    private static final String LAST_ID_KEY = "billing.lastId";

    private final BillingMapper billingMapper;
    private final int pageSize;
    private final Queue<Long> buffer = new ArrayDeque<>();

    private long lastId;

    /**
     * Constructs paging reader with mapper and page size.
     *
     * @param billingMapper mapper for billing target IDs
     * @param pageSize page size for each fetch
     */
    public BillingIdPagingReader(BillingMapper billingMapper, int pageSize) {
        this.billingMapper = billingMapper;
        this.pageSize = pageSize;
    }

    /**
     * Reads one billing ID and loads the next page when buffer is empty.
     *
     * @return next billing ID or {@code null} when no target remains
     */
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

    /**
     * Restores reader state from execution context.
     *
     * @param executionContext step execution context
     */
    @Override
    public void open(ExecutionContext executionContext) {
        lastId = executionContext.getLong(LAST_ID_KEY, 0L);
    }

    /**
     * Persists reader state into execution context.
     *
     * @param executionContext step execution context
     */
    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_ID_KEY, lastId);
    }

    /**
     * Clears in-memory buffer on stream close.
     */
    @Override
    public void close() {
        buffer.clear();
    }
}
