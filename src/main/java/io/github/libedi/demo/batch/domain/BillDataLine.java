package io.github.libedi.demo.batch.domain;

public record BillDataLine(
        long billingId,
        String payloadNdjson
) {
}
