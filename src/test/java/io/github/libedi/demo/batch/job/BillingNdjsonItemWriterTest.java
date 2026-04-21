package io.github.libedi.demo.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import io.github.libedi.demo.batch.mapper.customer.CustomerMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

/**
 * {@link BillingNdjsonItemWriter} 동작을 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class BillingNdjsonItemWriterTest {

    @Mock
    private BillDataMapper billDataMapper;

    @Mock
    private BillingMapper billingMapper;

    @Mock
    private CustomerMapper customerMapper;

    private BillingNdjsonItemWriter writer;

    /**
     * 각 테스트 실행 전에 Writer를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        writer = new BillingNdjsonItemWriter(
                billDataMapper,
                billingMapper,
                customerMapper,
                new ObjectMapper()
        );
    }

    /**
     * 조인 가능한 데이터 중 기존 저장 건을 제외하고 insert하며 processed를 갱신하는지 검증합니다.
     */
    @Test
    void writeInsertsOnlyMissingRowsAndMarksAllJoinableAsProcessed() {
        List<Long> inputIds = List.of(1L, 2L, 2L);

        when(billingMapper.findBillingHeaders(List.of(1L, 2L))).thenReturn(List.of(
                new BillingHeader(1L, "BILL-0001"),
                new BillingHeader(2L, "BILL-0002")
        ));
        when(billingMapper.findBillingDetails(List.of(1L, 2L))).thenReturn(List.of(
                new BillingDetail(1L, BigDecimal.valueOf(12000.50), LocalDate.of(2026, 4, 1)),
                new BillingDetail(2L, BigDecimal.valueOf(8000.00), LocalDate.of(2026, 4, 2))
        ));
        when(customerMapper.findCustomers(List.of(1L, 2L))).thenReturn(List.of(
                new CustomerInfo(1L, "Alice Kim", "alice@example.com"),
                new CustomerInfo(2L, "Bob Lee", "bob@example.com")
        ));
        when(billDataMapper.findExistingBillingIds(List.of(1L, 2L))).thenReturn(List.of(1L));

        writer.write(new Chunk<>(inputIds));

        verify(billDataMapper).insertBatch(argThat(inserted -> {
            assertThat(inserted).hasSize(1);
            BillDataLine line = inserted.getFirst();
            assertThat(line.billingId()).isEqualTo(2L);
            assertThat(line.payloadNdjson()).contains("\"billingNo\":\"BILL-0002\"");
            assertThat(line.payloadNdjson()).endsWith("\n");
            return true;
        }));

        verify(billingMapper).markProcessed(List.of(1L, 2L));
    }

    /**
     * 조인 가능한 데이터가 없으면 insert/processed 갱신이 수행되지 않는지 검증합니다.
     */
    @Test
    void writeSkipsWhenNoJoinableRows() {
        when(billingMapper.findBillingHeaders(List.of(3L))).thenReturn(List.of(
                new BillingHeader(3L, "BILL-0003")
        ));
        when(billingMapper.findBillingDetails(List.of(3L))).thenReturn(List.of());
        when(customerMapper.findCustomers(List.of(3L))).thenReturn(List.of(
                new CustomerInfo(3L, "Chris Park", "chris@example.com")
        ));

        writer.write(new Chunk<>(List.of(3L)));

        verify(billDataMapper, never()).findExistingBillingIds(anyList());
        verify(billDataMapper, never()).insertBatch(anyList());
        verify(billingMapper, never()).markProcessed(anyList());
    }
}
