package io.github.libedi.demo.batch.mapper.bill;

import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * billing 도메인 조회/수정용 매퍼입니다.
 */
public interface BillingMapper {

    /**
     * 마지막 ID 이후, 지정 상한 ID 이하의 미처리 청구 ID 페이지를 조회합니다.
     *
     * @param lastId 마지막 처리 청구 ID
     * @param maxId 이번 실행에서 허용할 최대 청구 ID
     * @param pageSize 조회할 최대 행 수
     * @return 대상 청구 ID 목록
     */
    @Select("""
            SELECT id
            FROM billing
            WHERE processed = FALSE
              AND id > #{lastId}
              AND id <= #{maxId}
            ORDER BY id
            LIMIT #{pageSize}
            """)
    List<Long> findTargetBillingIdsInRange(
            @Param("lastId") long lastId,
            @Param("maxId") long maxId,
            @Param("pageSize") int pageSize
    );

    /**
     * 청구 ID 목록으로 billing 헤더를 일괄 조회합니다.
     *
     * @param billingIds 청구 식별자 목록
     * @return billing 헤더 행 목록
     */
    @Select("""
            <script>
            SELECT id, billing_no
            FROM billing
            WHERE id IN
            <foreach collection='billingIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    List<BillingHeader> findBillingHeaders(@Param("billingIds") List<Long> billingIds);

    /**
     * 청구 ID 목록으로 billing 상세를 일괄 조회합니다.
     *
     * @param billingIds 청구 식별자 목록
     * @return billing 상세 행 목록
     */
    @Select("""
            <script>
            SELECT billing_id, amount, due_date
            FROM billing_detail
            WHERE billing_id IN
            <foreach collection='billingIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    List<BillingDetail> findBillingDetails(@Param("billingIds") List<Long> billingIds);

    /**
     * billing 행을 처리 완료 상태로 변경합니다.
     *
     * @param billingIds 업데이트할 청구 식별자 목록
     * @return 영향받은 행 수
     */
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

    /**
     * 처리 완료된 billing 행 수를 조회합니다.
     *
     * @return 처리 완료 행 수
     */
    @Select("SELECT COUNT(*) FROM billing WHERE processed = TRUE")
    long countProcessed();
}


