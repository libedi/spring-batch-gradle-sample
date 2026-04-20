package io.github.libedi.demo.batch.mapper.bill;

import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface BillingMapper {

    @Select("""
            SELECT id
            FROM billing
            WHERE processed = FALSE
              AND id > #{lastId}
            ORDER BY id
            LIMIT #{pageSize}
            """)
    List<Long> findTargetBillingIds(@Param("lastId") long lastId, @Param("pageSize") int pageSize);

    @Select("""
            SELECT id, billing_no
            FROM billing
            WHERE id = #{billingId}
            """)
    BillingHeader findBillingHeader(@Param("billingId") long billingId);

    @Select("""
            SELECT billing_id, amount, due_date
            FROM billing_detail
            WHERE billing_id = #{billingId}
            """)
    BillingDetail findBillingDetail(@Param("billingId") long billingId);

    @Update("""
            <script>
            UPDATE billing
            SET processed = TRUE
            WHERE id IN
            <foreach collection='billingIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    int markProcessed(@Param("billingIds") List<Long> billingIds);

    @Select("SELECT COUNT(*) FROM billing WHERE processed = TRUE")
    long countProcessed();
}
