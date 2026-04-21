package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/**
 * Processor가 전달한 조합 결과를 저장하고 처리 상태를 갱신하는 Writer입니다.
 */
public class BillingNdjsonItemWriter implements ItemWriter<BillingJoinChunk> {

    private final BillDataMapper billDataMapper;
    private final BillingMapper billingMapper;

    /**
     * 저장/상태갱신 매퍼를 주입받아 Writer를 생성합니다.
     *
     * @param billDataMapper NDJSON 저장 매퍼
     * @param billingMapper billing 조회/갱신 매퍼
     */
    public BillingNdjsonItemWriter(
            BillDataMapper billDataMapper,
            BillingMapper billingMapper
    ) {
        this.billDataMapper = billDataMapper;
        this.billingMapper = billingMapper;
    }

    /**
     * Processor 결과를 모아 NDJSON 저장 및 processed 상태 갱신을 수행합니다.
     *
     * @param chunk 현재 청크 아이템
     */
    @Override
    public void write(Chunk<? extends BillingJoinChunk> chunk) {
        List<BillDataLine> allLines = new ArrayList<>();
        Set<Long> allProcessedIds = new LinkedHashSet<>();
        for (BillingJoinChunk joinedChunk : chunk.getItems()) {
            if (joinedChunk == null) {
                continue;
            }
            allLines.addAll(joinedChunk.lines());
            allProcessedIds.addAll(joinedChunk.processedIds());
        }

        if (!allLines.isEmpty()) {
            billDataMapper.insertBatch(allLines);
        }
        if (!allProcessedIds.isEmpty()) {
            billingMapper.markProcessed(List.copyOf(allProcessedIds));
        }
    }
}
