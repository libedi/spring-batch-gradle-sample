package io.github.libedi.demo.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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
 * {@link BillingIdPagingReader}의 페이징/재시작 동작을 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class BillingIdPagingReaderTest {

    @Mock
    private BillingMapper billingMapper;

    private BillingIdPagingReader reader;

    /**
     * 테스트마다 Reader를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        reader = new BillingIdPagingReader(billingMapper, 2);
    }

    /**
     * 페이지를 순차 로딩하여 ID를 끝까지 읽는지 검증합니다.
     */
    @Test
    void readLoadsNextPageWhenBufferBecomesEmpty() {
        when(billingMapper.findTargetBillingIds(0L, 2)).thenReturn(List.of(1L, 2L));
        when(billingMapper.findTargetBillingIds(2L, 2)).thenReturn(List.of(3L));
        when(billingMapper.findTargetBillingIds(3L, 2)).thenReturn(List.of());

        reader.open(new ExecutionContext());

        assertThat(reader.read()).isEqualTo(1L);
        assertThat(reader.read()).isEqualTo(2L);
        assertThat(reader.read()).isEqualTo(3L);
        assertThat(reader.read()).isNull();
    }

    /**
     * 실행 컨텍스트의 lastId부터 재시작하는지 검증합니다.
     */
    @Test
    void openRestoresLastIdFromExecutionContext() {
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.putLong("billing.lastId", 10L);
        when(billingMapper.findTargetBillingIds(10L, 2)).thenReturn(List.of(11L, 12L));

        reader.open(executionContext);

        assertThat(reader.read()).isEqualTo(11L);
        assertThat(reader.read()).isEqualTo(12L);
    }

    /**
     * update가 최신 lastId를 실행 컨텍스트에 저장하는지 검증합니다.
     */
    @Test
    void updateStoresLatestLastIdInExecutionContext() {
        when(billingMapper.findTargetBillingIds(0L, 2)).thenReturn(List.of(7L, 8L));

        reader.open(new ExecutionContext());
        assertThat(reader.read()).isEqualTo(7L);

        ExecutionContext executionContext = new ExecutionContext();
        reader.update(executionContext);

        assertThat(executionContext.getLong("billing.lastId")).isEqualTo(8L);
    }

    /**
     * close 호출 시 내부 버퍼를 비우는지 검증합니다.
     */
    @Test
    void closeClearsInternalBuffer() {
        when(billingMapper.findTargetBillingIds(0L, 2)).thenReturn(List.of(1L, 2L));
        when(billingMapper.findTargetBillingIds(2L, 2)).thenReturn(List.of());

        reader.open(new ExecutionContext());
        assertThat(reader.read()).isEqualTo(1L);
        reader.close();

        assertThat(reader.read()).isNull();
        verify(billingMapper).findTargetBillingIds(2L, 2);
    }
}
