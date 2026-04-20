package io.github.libedi.demo.batch.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Billing detail snapshot joined by billing ID.
 *
 * @param billingId billing identifier
 * @param amount billed amount
 * @param dueDate due date
 */
public record BillingDetail(
        long billingId,
        BigDecimal amount,
        LocalDate dueDate
) {
}
