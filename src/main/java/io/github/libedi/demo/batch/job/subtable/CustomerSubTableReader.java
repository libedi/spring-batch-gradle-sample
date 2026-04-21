package io.github.libedi.demo.batch.job.subtable;

import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.mapper.customer.CustomerMapper;
import java.util.List;

/**
 * customer 서브테이블 조회 구현체입니다.
 */
public class CustomerSubTableReader implements SubTableReader<CustomerInfo> {

    private final CustomerMapper customerMapper;

    /**
     * 매퍼를 주입받아 조회 구현체를 생성합니다.
     *
     * @param customerMapper customer 조회 매퍼
     */
    public CustomerSubTableReader(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    /**
     * customer 행 목록을 조회합니다.
     *
     * @param billingIds 조회 대상 billing ID 목록
     * @return 조회된 고객 목록
     */
    @Override
    public List<CustomerInfo> readByBillingIds(List<Long> billingIds) {
        return customerMapper.findCustomers(billingIds);
    }
}
