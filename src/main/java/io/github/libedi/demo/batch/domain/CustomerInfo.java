package io.github.libedi.demo.batch.domain;

import io.github.libedi.demo.batch.job.subtable.SubTableRecord;

/**
 * 청구 ID로 조인된 고객 스냅샷입니다.
 *
 * @param billingId 청구 식별자
 * @param customerName 고객 표시 이름
 * @param email 고객 이메일
 */
public record CustomerInfo(
        long billingId,
        String customerName,
        String email
) implements SubTableRecord {
}


