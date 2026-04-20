package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * bill 데이터소스에서 청구 ID를 페이지 단위로 읽는 ItemReader입니다.
 */
public class BillingIdPagingReader implements ItemStreamReader<Long> {

    private static final String LAST_ID_KEY = "billing.lastId";

    private final BillingMapper billingMapper;
    private final int pageSize;
    private final Queue<Long> buffer = new ArrayDeque<>();

    private long lastId;

    /**
     * 매퍼와 페이지 크기로 페이징 리더를 생성합니다.
     *
     * @param billingMapper 청구 대상 ID 조회 매퍼
     * @param pageSize 조회당 페이지 크기
     */
    public BillingIdPagingReader(BillingMapper billingMapper, int pageSize) {
        this.billingMapper = billingMapper;
        this.pageSize = pageSize;
    }

    /**
     * 청구 ID 1건을 읽고 버퍼가 비면 다음 페이지를 로드합니다.
     *
     * @return 다음 청구 ID, 더 이상 대상이 없으면 {@code null}
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
     * 실행 컨텍스트에서 리더 상태를 복원합니다.
     *
     * @param executionContext Step 실행 컨텍스트
     */
    @Override
    public void open(ExecutionContext executionContext) {
        lastId = executionContext.getLong(LAST_ID_KEY, 0L);
    }

    /**
     * 리더 상태를 실행 컨텍스트에 저장합니다.
     *
     * @param executionContext Step 실행 컨텍스트
     */
    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_ID_KEY, lastId);
    }

    /**
     * 스트림 종료 시 메모리 버퍼를 비웁니다.
     */
    @Override
    public void close() {
        buffer.clear();
    }
}


