package io.github.libedi.demo.batch.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 청구 ID로 조인된 billing 상세 스냅샷입니다.
 *
 * @param billingId 청구 식별자
 * @param amount 청구 금액
 * @param dueDate 납기일
 */
public record BillingDetail(
        long billingId,
        BigDecimal amount,
        LocalDate dueDate
) {
}


