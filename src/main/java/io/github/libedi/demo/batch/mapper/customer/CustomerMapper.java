package io.github.libedi.demo.batch.mapper.customer;

import io.github.libedi.demo.batch.domain.CustomerInfo;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * customer 도메인 조회용 매퍼입니다.
 */
public interface CustomerMapper {

    /**
     * 청구 ID 목록으로 고객 정보를 일괄 조회합니다.
     *
     * @param billingIds 청구 식별자 목록
     * @return 고객 정보 행 목록
     */
    @Select("""
            <script>
            SELECT billing_id, customer_name, email
            FROM customer
            WHERE billing_id IN
            <foreach collection='billingIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    List<CustomerInfo> findCustomers(@Param("billingIds") List<Long> billingIds);
}


