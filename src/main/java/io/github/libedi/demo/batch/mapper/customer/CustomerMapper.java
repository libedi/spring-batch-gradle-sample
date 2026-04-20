package io.github.libedi.demo.batch.mapper.customer;

import io.github.libedi.demo.batch.domain.CustomerInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CustomerMapper {

    @Select("""
            SELECT billing_id, customer_name, email
            FROM customer
            WHERE billing_id = #{billingId}
            """)
    CustomerInfo findCustomer(@Param("billingId") long billingId);
}
