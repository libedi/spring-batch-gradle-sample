package io.github.libedi.demo.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.job.subtable.SubTableReader;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link BillingJoinChunkProcessor} 동작을 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class BillingJoinChunkProcessorTest {

    @Mock
    private BillingMapper billingMapper;

    @Mock
    private SubTableReader<BillingDetail> billingDetailSubTableReader;

    @Mock
    private SubTableReader<CustomerInfo> customerSubTableReader;

    @Mock
    private BillDataMapper billDataMapper;

    private BillingJoinChunkProcessor processor;

    /**
     * 테스트마다 Processor를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        processor = new BillingJoinChunkProcessor(
                billingMapper,
                billingDetailSubTableReader,
                customerSubTableReader,
                billDataMapper,
                new ObjectMapper()
        );
    }

    /**
     * 조인 가능한 데이터 중 기존 저장 건을 제외해 저장 대상을 만드는지 검증합니다.
     */
    @Test
    void processBuildsJoinedChunkWithInsertTargets() {
        List<Long> inputIds = List.of(1L, 2L, 2L);
        List<Long> deduplicatedIds = List.of(1L, 2L);

        when(billingMapper.findBillingHeaders(deduplicatedIds)).thenReturn(List.of(
                new BillingHeader(1L, "BILL-0001"),
                new BillingHeader(2L, "BILL-0002")
        ));
        when(billingDetailSubTableReader.readByBillingIds(deduplicatedIds)).thenReturn(List.of(
                new BillingDetail(1L, BigDecimal.valueOf(12000.50), LocalDate.of(2026, 4, 1)),
                new BillingDetail(2L, BigDecimal.valueOf(8000.00), LocalDate.of(2026, 4, 2))
        ));
        when(customerSubTableReader.readByBillingIds(deduplicatedIds)).thenReturn(List.of(
                new CustomerInfo(1L, "Alice Kim", "alice@example.com"),
                new CustomerInfo(2L, "Bob Lee", "bob@example.com")
        ));
        when(billDataMapper.findExistingBillingIds(deduplicatedIds)).thenReturn(List.of(1L));

        BillingJoinChunk result = processor.process(inputIds);

        assertThat(result).isNotNull();
        assertThat(result.processedIds()).containsExactly(1L, 2L);
        assertThat(result.lines()).hasSize(1);
        assertThat(result.lines().getFirst().billingId()).isEqualTo(2L);
        assertThat(result.lines().getFirst().payloadNdjson()).contains("\"billingNo\":\"BILL-0002\"");
    }

    /**
     * 조인 가능한 데이터가 없으면 null을 반환하는지 검증합니다.
     */
    @Test
    void processReturnsNullWhenNoJoinableRows() {
        when(billingMapper.findBillingHeaders(List.of(3L))).thenReturn(List.of(
                new BillingHeader(3L, "BILL-0003")
        ));
        when(billingDetailSubTableReader.readByBillingIds(List.of(3L))).thenReturn(List.of());
        when(customerSubTableReader.readByBillingIds(List.of(3L))).thenReturn(List.of(
                new CustomerInfo(3L, "Chris Park", "chris@example.com")
        ));

        BillingJoinChunk result = processor.process(List.of(3L));

        assertThat(result).isNull();
    }
}
