package io.github.libedi.demo.batch.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingDetail(
        long billingId,
        BigDecimal amount,
        LocalDate dueDate
) {
}
