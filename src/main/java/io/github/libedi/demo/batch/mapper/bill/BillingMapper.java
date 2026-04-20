package io.github.libedi.demo.batch.mapper.bill;

import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper for billing domain read/update operations.
 */
public interface BillingMapper {

    /**
     * Finds next page of unprocessed billing IDs after given last ID.
     *
     * @param lastId last processed billing ID
     * @param pageSize max rows to fetch
     * @return target billing IDs
     */
    @Select("""
            SELECT id
            FROM billing
            WHERE processed = FALSE
              AND id > #{lastId}
            ORDER BY id
            LIMIT #{pageSize}
            """)
    List<Long> findTargetBillingIds(@Param("lastId") long lastId, @Param("pageSize") int pageSize);

    /**
     * Finds billing header by billing ID.
     *
     * @param billingId billing identifier
     * @return billing header row
     */
    @Select("""
            SELECT id, billing_no
            FROM billing
            WHERE id = #{billingId}
            """)
    BillingHeader findBillingHeader(@Param("billingId") long billingId);

    /**
     * Finds billing detail by billing ID.
     *
     * @param billingId billing identifier
     * @return billing detail row
     */
    @Select("""
            SELECT billing_id, amount, due_date
            FROM billing_detail
            WHERE billing_id = #{billingId}
            """)
    BillingDetail findBillingDetail(@Param("billingId") long billingId);

    /**
     * Marks billing rows as processed.
     *
     * @param billingIds billing identifiers to update
     * @return affected row count
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
     * Counts processed billing rows.
     *
     * @return processed row count
     */
    @Select("SELECT COUNT(*) FROM billing WHERE processed = TRUE")
    long countProcessed();
}
