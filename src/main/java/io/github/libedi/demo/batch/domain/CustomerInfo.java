package io.github.libedi.demo.batch.domain;

public record CustomerInfo(
        long billingId,
        String customerName,
        String email
) {
}
