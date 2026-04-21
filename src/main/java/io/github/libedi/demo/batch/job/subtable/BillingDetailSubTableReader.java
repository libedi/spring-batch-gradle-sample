package io.github.libedi.demo.batch.job.subtable;

import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.List;

/**
 * billing_detail 서브테이블 조회 구현체입니다.
 */
public class BillingDetailSubTableReader implements SubTableReader<BillingDetail> {

    private final BillingMapper billingMapper;

    /**
     * 매퍼를 주입받아 조회 구현체를 생성합니다.
     *
     * @param billingMapper billing 조회 매퍼
     */
    public BillingDetailSubTableReader(BillingMapper billingMapper) {
        this.billingMapper = billingMapper;
    }

    /**
     * billing_detail 행 목록을 조회합니다.
     *
     * @param billingIds 조회 대상 billing ID 목록
     * @return 조회된 상세 목록
     */
    @Override
    public List<BillingDetail> readByBillingIds(List<Long> billingIds) {
        return billingMapper.findBillingDetails(billingIds);
    }
}
