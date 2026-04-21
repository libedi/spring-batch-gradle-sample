package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.List;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * Keyset 방식으로 billing ID 목록을 페이지 단위로 읽는 Reader입니다.
 */
public class BillingKeysetChunkReader implements ItemStreamReader<List<Long>> {

    private static final String LAST_ID_KEY = "billing.lastId";

    private final BillingMapper billingMapper;
    private final int pageSize;
    private final long maxId;

    private long lastId;

    /**
     * 매퍼와 페이지 조건으로 Keyset Reader를 생성합니다.
     *
     * @param billingMapper billing 대상 ID 조회 매퍼
     * @param pageSize 페이지 크기
     * @param maxId 현재 실행에서 처리할 상한 billing ID
     */
    public BillingKeysetChunkReader(BillingMapper billingMapper, int pageSize, long maxId) {
        this.billingMapper = billingMapper;
        this.pageSize = pageSize;
        this.maxId = maxId;
    }

    /**
     * 현재 lastId 이후 상한(maxId) 이하 ID 페이지를 읽습니다.
     *
     * @return 다음 ID 페이지, 대상이 없으면 {@code null}
     */
    @Override
    public List<Long> read() {
        List<Long> billingIds = billingMapper.findTargetBillingIdsInRange(lastId, maxId, pageSize);
        if (billingIds.isEmpty()) {
            return null;
        }
        lastId = billingIds.getLast();
        return billingIds;
    }

    /**
     * 실행 컨텍스트에서 lastId 상태를 복원합니다.
     *
     * @param executionContext Step 실행 컨텍스트
     */
    @Override
    public void open(ExecutionContext executionContext) {
        lastId = executionContext.getLong(LAST_ID_KEY, 0L);
    }

    /**
     * 현재 lastId 상태를 실행 컨텍스트에 저장합니다.
     *
     * @param executionContext Step 실행 컨텍스트
     */
    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_ID_KEY, lastId);
    }

    /**
     * 리더 종료 시 정리 동작은 필요하지 않습니다.
     */
    @Override
    public void close() {
        // no-op
    }
}
