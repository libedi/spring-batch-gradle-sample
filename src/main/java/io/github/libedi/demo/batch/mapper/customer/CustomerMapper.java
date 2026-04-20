package io.github.libedi.demo.batch.mapper.customer;

import io.github.libedi.demo.batch.domain.CustomerInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper for customer domain read operations.
 */
public interface CustomerMapper {

    /**
     * Finds customer info by billing ID.
     *
     * @param billingId billing identifier
     * @return customer info row
     */
    @Select("""
            SELECT billing_id, customer_name, email
            FROM customer
            WHERE billing_id = #{billingId}
            """)
    CustomerInfo findCustomer(@Param("billingId") long billingId);
}
