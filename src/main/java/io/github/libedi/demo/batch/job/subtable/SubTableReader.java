package io.github.libedi.demo.batch.job.subtable;

import java.util.List;

/**
 * 서브테이블 데이터를 billing ID 목록 기준으로 읽는 조회 컴포넌트 계약입니다.
 *
 * @param <T> 조회 결과 타입
 */
public interface SubTableReader<T extends SubTableRecord> {

    /**
     * billing ID 목록에 해당하는 서브테이블 행을 조회합니다.
     *
     * @param billingIds 조회 대상 billing ID 목록
     * @return 조회된 행 목록
     */
    List<T> readByBillingIds(List<Long> billingIds);
}
