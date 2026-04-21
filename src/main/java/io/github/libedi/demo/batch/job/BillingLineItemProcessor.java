package io.github.libedi.demo.batch.job;

import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * 청크 처리에 사용할 청구 ID를 정규화하는 Processor입니다.
 */
public class BillingLineItemProcessor implements ItemProcessor<Long, Long> {

    /**
     * 유효한 청구 ID만 다음 단계로 전달합니다.
     *
     * @param billingId 청구 식별자
     * @return 유효한 청구 식별자, 유효하지 않으면 {@code null}
     */
    @Override
    public Long process(Long billingId) {
        if (billingId == null || billingId <= 0L) {
            return null;
        }
        return billingId;
    }
}
