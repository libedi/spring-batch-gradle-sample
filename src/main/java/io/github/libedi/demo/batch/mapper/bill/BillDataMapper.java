package io.github.libedi.demo.batch.mapper.bill;

import io.github.libedi.demo.batch.domain.BillDataLine;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code bill_data} 조회/저장용 매퍼입니다.
 */
public interface BillDataMapper {

    /**
     * 생성된 NDJSON 라인을 배치로 저장합니다.
     *
     * @param items 저장할 청구 데이터 라인 목록
     * @return 영향받은 행 수
     */
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

    /**
     * {@code bill_data} 행 수를 조회합니다.
     *
     * @return 행 수
     */
    @Select("SELECT COUNT(*) FROM bill_data")
    long countBillData();

    /**
     * 청구 ID로 NDJSON 페이로드를 조회합니다.
     *
     * @param billingId 청구 식별자
     * @return NDJSON 페이로드
     */
    @Select("SELECT payload_ndjson FROM bill_data WHERE billing_id = #{billingId}")
    String findPayloadByBillingId(@Param("billingId") long billingId);
}


