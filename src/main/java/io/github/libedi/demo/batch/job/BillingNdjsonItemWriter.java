package io.github.libedi.demo.batch.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.libedi.demo.batch.domain.BillDataLine;
import io.github.libedi.demo.batch.domain.BillingDetail;
import io.github.libedi.demo.batch.domain.BillingHeader;
import io.github.libedi.demo.batch.domain.CustomerInfo;
import io.github.libedi.demo.batch.mapper.bill.BillDataMapper;
import io.github.libedi.demo.batch.mapper.bill.BillingMapper;
import io.github.libedi.demo.batch.mapper.customer.CustomerMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Strings;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/**
 * 청구/상세/고객 데이터를 청크 단위로 조합해 NDJSON를 저장하는 Writer입니다.
 */
public class BillingNdjsonItemWriter implements ItemWriter<Long> {

    private final BillDataMapper billDataMapper;
    private final BillingMapper billingMapper;
    private final CustomerMapper customerMapper;
    private final ObjectMapper objectMapper;

    /**
     * 매퍼와 직렬화기를 주입받아 Writer를 생성합니다.
     *
     * @param billDataMapper NDJSON 저장 매퍼
     * @param billingMapper billing 조회/갱신 매퍼
     * @param customerMapper customer 조회 매퍼
     * @param objectMapper JSON 직렬화기
     */
    public BillingNdjsonItemWriter(
            BillDataMapper billDataMapper,
            BillingMapper billingMapper,
            CustomerMapper customerMapper,
            ObjectMapper objectMapper
    ) {
        this.billDataMapper = billDataMapper;
        this.billingMapper = billingMapper;
        this.customerMapper = customerMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 청크 ID를 배치 조회로 조합하여 NDJSON 저장 및 processed 상태 갱신을 수행합니다.
     *
     * @param chunk 현재 청크 아이템
     */
    @Override
    public void write(Chunk<? extends Long> chunk) {
        List<Long> billingIds = deduplicate(chunk.getItems());
        if (billingIds.isEmpty()) {
            return;
        }

        Map<Long, BillingHeader> headersById = toHeaderMap(billingMapper.findBillingHeaders(billingIds));
        Map<Long, BillingDetail> detailsById = toDetailMap(billingMapper.findBillingDetails(billingIds));
        Map<Long, CustomerInfo> customersById = toCustomerMap(customerMapper.findCustomers(billingIds));

        List<Long> joinableIds = new ArrayList<>();
        List<BillDataLine> lines = new ArrayList<>();
        for (Long billingId : billingIds) {
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
            return;
        }

        Set<Long> existingIds = new LinkedHashSet<>(billDataMapper.findExistingBillingIds(joinableIds));
        List<BillDataLine> insertTargets = lines.stream()
                .filter(line -> !existingIds.contains(line.billingId()))
                .toList();
        if (!insertTargets.isEmpty()) {
            billDataMapper.insertBatch(insertTargets);
        }

        billingMapper.markProcessed(joinableIds);
    }

    /**
     * 중복 ID를 제거하며 입력 순서를 유지합니다.
     *
     * @param inputIds 청크 입력 ID 목록
     * @return 중복 제거된 ID 목록
     */
    private List<Long> deduplicate(List<? extends Long> inputIds) {
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
