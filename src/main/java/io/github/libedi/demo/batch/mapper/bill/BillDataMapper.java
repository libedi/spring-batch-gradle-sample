package io.github.libedi.demo.batch.mapper.bill;

import io.github.libedi.demo.batch.domain.BillDataLine;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BillDataMapper {

    @Insert("""
            <script>
            INSERT INTO bill_data (billing_id, payload_ndjson, created_at)
            VALUES
            <foreach collection='items' item='item' separator=','>
                (#{item.billingId}, #{item.payloadNdjson}, CURRENT_TIMESTAMP)
            </foreach>
            </script>
            """)
    int insertBatch(@Param("items") List<BillDataLine> items);

    @Select("SELECT COUNT(*) FROM bill_data")
    long countBillData();

    @Select("SELECT payload_ndjson FROM bill_data WHERE billing_id = #{billingId}")
    String findPayloadByBillingId(@Param("billingId") long billingId);
}
