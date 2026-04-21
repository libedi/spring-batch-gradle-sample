package io.github.libedi.demo.batch.job;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
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

    private BillingNdjsonItemWriter writer;

    /**
     * 각 테스트 실행 전에 Writer를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        writer = new BillingNdjsonItemWriter(billDataMapper, billingMapper);
    }

    /**
     * Processor 결과를 합쳐 insert/processed 갱신을 수행하는지 검증합니다.
     */
    @Test
    void writePersistsJoinedChunkAndMarksProcessed() {
        BillingJoinChunk joinedChunk = new BillingJoinChunk(
                List.of(new BillDataLine(2L, "{\"billingId\":2}\n")),
                List.of(1L, 2L)
        );

        writer.write(new Chunk<>(List.of(joinedChunk)));

        verify(billDataMapper).insertBatch(joinedChunk.lines());
        verify(billingMapper).markProcessed(joinedChunk.processedIds());
    }

    /**
     * 저장/갱신 대상이 없으면 DB 쓰기를 수행하지 않는지 검증합니다.
     */
    @Test
    void writeSkipsWhenNoTargets() {
        BillingJoinChunk emptyChunk = new BillingJoinChunk(List.of(), List.of());

        writer.write(new Chunk<>(List.of(emptyChunk)));

        verify(billDataMapper, never()).insertBatch(anyList());
        verify(billingMapper, never()).markProcessed(anyList());
    }
}
