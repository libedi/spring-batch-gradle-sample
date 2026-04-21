package io.github.libedi.demo.batch.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.job.subtable.SubTableReader;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Strings;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * 청크 단위 billing ID 목록을 조합 데이터로 변환하는 Processor입니다.
 */
public class BillingJoinChunkProcessor implements ItemProcessor<List<Long>, BillingJoinChunk> {

    private final BillingMapper billingMapper;
    private final SubTableReader<BillingDetail> billingDetailSubTableReader;
    private final SubTableReader<CustomerInfo> customerSubTableReader;
    private final BillDataMapper billDataMapper;
    private final ObjectMapper objectMapper;

    /**
     * 조합 처리에 필요한 매퍼와 직렬화기를 주입받아 Processor를 생성합니다.
     *
     * @param billingMapper billing 조회/갱신 매퍼
     * @param billingDetailSubTableReader billing_detail 조회 구현체
     * @param customerSubTableReader customer 조회 구현체
     * @param billDataMapper bill_data 조회 매퍼
     * @param objectMapper JSON 직렬화기
     */
    public BillingJoinChunkProcessor(
            BillingMapper billingMapper,
            SubTableReader<BillingDetail> billingDetailSubTableReader,
            SubTableReader<CustomerInfo> customerSubTableReader,
            BillDataMapper billDataMapper,
            ObjectMapper objectMapper
    ) {
        this.billingMapper = billingMapper;
        this.billingDetailSubTableReader = billingDetailSubTableReader;
        this.customerSubTableReader = customerSubTableReader;
        this.billDataMapper = billDataMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 청크 ID를 배치 조인해 저장 대상/처리완료 대상 묶음을 생성합니다.
     *
     * @param billingIds 페이지 단위 billing ID 목록
     * @return 저장/갱신 대상 묶음, 유효 대상이 없으면 {@code null}
     */
    @Override
    public BillingJoinChunk process(List<Long> billingIds) {
        List<Long> deduplicatedIds = deduplicate(billingIds);
        if (deduplicatedIds.isEmpty()) {
            return null;
        }

        Map<Long, BillingHeader> headersById = toHeaderMap(billingMapper.findBillingHeaders(deduplicatedIds));
        Map<Long, BillingDetail> detailsById = toDetailMap(billingDetailSubTableReader.readByBillingIds(deduplicatedIds));
        Map<Long, CustomerInfo> customersById = toCustomerMap(customerSubTableReader.readByBillingIds(deduplicatedIds));

        List<Long> joinableIds = new ArrayList<>();
        List<BillDataLine> lines = new ArrayList<>();
        for (Long billingId : deduplicatedIds) {
            BillingHeader header = headersById.get(billingId);
            BillingDetail detail = detailsById.get(billingId);
            CustomerInfo customer = customersById.get(billingId);
            if (header == null || detail == null || customer == null) {
                continue;
            }

            joinableIds.add(billingId);
            lines.add(new BillDataLine(header.id(), toJsonLine(header, detail, customer)));
        }

        if (joinableIds.isEmpty()) {
            return null;
        }

        Set<Long> existingIds = new LinkedHashSet<>(billDataMapper.findExistingBillingIds(joinableIds));
        List<BillDataLine> insertTargets = lines.stream()
                .filter(line -> !existingIds.contains(line.billingId()))
                .toList();
        return new BillingJoinChunk(insertTargets, joinableIds);
    }

    /**
     * 중복 ID를 제거하며 입력 순서를 유지합니다.
     *
     * @param inputIds 입력 ID 목록
     * @return 중복 제거된 ID 목록
     */
    private List<Long> deduplicate(List<Long> inputIds) {
        return new ArrayList<>(new LinkedHashSet<>(inputIds));
    }

    /**
     * billing 헤더 목록을 ID 인덱스 맵으로 변환합니다.
     *
     * @param headers billing 헤더 목록
     * @return ID 기준 맵
     */
    private Map<Long, BillingHeader> toHeaderMap(List<BillingHeader> headers) {
        Map<Long, BillingHeader> map = new LinkedHashMap<>();
        for (BillingHeader header : headers) {
            map.put(header.id(), header);
        }
        return map;
    }

    /**
     * billing 상세 목록을 ID 인덱스 맵으로 변환합니다.
     *
     * @param details billing 상세 목록
     * @return ID 기준 맵
     */
    private Map<Long, BillingDetail> toDetailMap(List<BillingDetail> details) {
        Map<Long, BillingDetail> map = new LinkedHashMap<>();
        for (BillingDetail detail : details) {
            map.put(detail.billingId(), detail);
        }
        return map;
    }

    /**
     * customer 목록을 ID 인덱스 맵으로 변환합니다.
     *
     * @param customers customer 목록
     * @return ID 기준 맵
     */
    private Map<Long, CustomerInfo> toCustomerMap(List<CustomerInfo> customers) {
        Map<Long, CustomerInfo> map = new LinkedHashMap<>();
        for (CustomerInfo customer : customers) {
            map.put(customer.billingId(), customer);
        }
        return map;
    }

    /**
     * 조인된 도메인 객체를 NDJSON 한 줄로 직렬화합니다.
     *
     * @param header billing 헤더
     * @param detail billing 상세
     * @param customer 고객 정보
     * @return 개행 포함 NDJSON 라인
     */
    private String toJsonLine(BillingHeader header, BillingDetail detail, CustomerInfo customer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("billingId", header.id());
        payload.put("billingNo", header.billingNo());
        payload.put("amount", detail.amount());
        payload.put("dueDate", Objects.toString(detail.dueDate(), ""));
        payload.put("customerName", customer.customerName());
        payload.put("email", customer.email());

        try {
            String json = objectMapper.writeValueAsString(payload);
            if (Strings.CS.endsWith(json, "\n")) {
                return json;
            }
            return json + "\n";
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize bill payload as NDJSON.", exception);
        }
    }
}
