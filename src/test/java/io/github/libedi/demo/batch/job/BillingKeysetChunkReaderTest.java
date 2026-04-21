package io.github.libedi.demo.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * {@link BillingKeysetChunkReader}의 Keyset/재시작 동작을 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class BillingKeysetChunkReaderTest {

    @Mock
    private BillingMapper billingMapper;

    private BillingKeysetChunkReader reader;

    /**
     * 테스트마다 Reader를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        reader = new BillingKeysetChunkReader(billingMapper, 2, 100L);
    }

    /**
     * lastId 기반 Keyset 페이지를 순차로 읽는지 검증합니다.
     */
    @Test
    void readUsesKeysetRangePagination() {
        when(billingMapper.findTargetBillingIdsInRange(0L, 100L, 2)).thenReturn(List.of(1L, 2L));
        when(billingMapper.findTargetBillingIdsInRange(2L, 100L, 2)).thenReturn(List.of(3L));
        when(billingMapper.findTargetBillingIdsInRange(3L, 100L, 2)).thenReturn(List.of());

        reader.open(new ExecutionContext());

        assertThat(reader.read()).containsExactly(1L, 2L);
        assertThat(reader.read()).containsExactly(3L);
        assertThat(reader.read()).isNull();
    }

    /**
     * 실행 컨텍스트에 저장된 lastId에서 재시작하는지 검증합니다.
     */
    @Test
    void openRestoresLastIdFromExecutionContext() {
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.putLong("billing.lastId", 10L);
        when(billingMapper.findTargetBillingIdsInRange(10L, 100L, 2)).thenReturn(List.of(11L, 12L));

        reader.open(executionContext);

        assertThat(reader.read()).containsExactly(11L, 12L);
    }

    /**
     * update가 최신 lastId를 저장하는지 검증합니다.
     */
    @Test
    void updateStoresLatestLastId() {
        when(billingMapper.findTargetBillingIdsInRange(0L, 100L, 2)).thenReturn(List.of(7L, 8L));

        reader.open(new ExecutionContext());
        assertThat(reader.read()).containsExactly(7L, 8L);

        ExecutionContext executionContext = new ExecutionContext();
        reader.update(executionContext);

        assertThat(executionContext.getLong("billing.lastId")).isEqualTo(8L);
    }
}
