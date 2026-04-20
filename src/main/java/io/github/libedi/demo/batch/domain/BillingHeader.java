package io.github.libedi.demo.batch.domain;

/**
 * 청구 건의 billing 헤더 스냅샷입니다.
 *
 * @param id 청구 식별자
 * @param billingNo 청구 번호
 */
public record BillingHeader(
        long id,
        String billingNo
) {
}


