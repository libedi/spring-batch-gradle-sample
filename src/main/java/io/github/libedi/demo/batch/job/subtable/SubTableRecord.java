package io.github.libedi.demo.batch.job.subtable;

/**
 * billing ID를 키로 갖는 서브테이블 조회 결과 공통 계약입니다.
 */
public interface SubTableRecord {

    /**
     * 조인 기준 billing ID를 반환합니다.
     *
     * @return billing ID
     */
    long billingId();
}
