package io.github.libedi.demo.batch.mapper.customer;

import io.github.libedi.demo.batch.domain.CustomerInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * customer 도메인 조회용 매퍼입니다.
 */
public interface CustomerMapper {

    /**
     * 청구 ID로 고객 정보를 조회합니다.
     *
     * @param billingId 청구 식별자
     * @return 고객 정보 행
     */
    @Select("""
            SELECT billing_id, customer_name, email
            FROM customer
            WHERE billing_id = #{billingId}
            """)
    CustomerInfo findCustomer(@Param("billingId") long billingId);
}


