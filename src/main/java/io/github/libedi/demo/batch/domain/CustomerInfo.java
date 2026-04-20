package io.github.libedi.demo.batch.domain;

/**
 * Customer snapshot joined by billing ID.
 *
 * @param billingId billing identifier
 * @param customerName customer display name
 * @param email customer email
 */
public record CustomerInfo(
        long billingId,
        String customerName,
        String email
) {
}
