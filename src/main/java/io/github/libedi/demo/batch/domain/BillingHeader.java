package io.github.libedi.demo.batch.domain;

/**
 * Billing header snapshot for a billing record.
 *
 * @param id billing identifier
 * @param billingNo billing number
 */
public record BillingHeader(
        long id,
        String billingNo
) {
}
