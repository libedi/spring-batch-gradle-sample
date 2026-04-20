package io.github.libedi.demo.batch.domain;

/**
 * NDJSON output line persisted in {@code bill_data}.
 *
 * @param billingId billing identifier
 * @param payloadNdjson ndjson payload text
 */
public record BillDataLine(
        long billingId,
        String payloadNdjson
) {
}
