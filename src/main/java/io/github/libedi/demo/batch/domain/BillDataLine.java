package io.github.libedi.demo.batch.domain;

/**
 * {@code bill_data}에 저장되는 NDJSON 출력 라인입니다.
 *
 * @param billingId 청구 식별자
 * @param payloadNdjson NDJSON 페이로드 문자열
 */
public record BillDataLine(
        long billingId,
        String payloadNdjson
) {
}


