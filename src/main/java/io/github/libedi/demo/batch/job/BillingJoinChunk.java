package io.github.libedi.demo.batch.job;

import io.github.libedi.demo.batch.domain.BillDataLine;
import java.util.List;

/**
 * Processor가 생성한 NDJSON 저장 대상과 처리 완료 대상 ID 묶음입니다.
 *
 * @param lines 저장할 NDJSON 라인 목록
 * @param processedIds 처리 완료로 표시할 billing ID 목록
 */
public record BillingJoinChunk(
        List<BillDataLine> lines,
        List<Long> processedIds
) {
}
