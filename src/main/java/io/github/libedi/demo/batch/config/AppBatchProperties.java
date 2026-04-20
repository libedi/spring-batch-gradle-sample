package io.github.libedi.demo.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.batch}에 바인딩되는 배치 실행 속성입니다.
 *
 * @param chunkSize 처리 Step의 청크 크기
 * @param pageSize ID 페이징 리더의 페이지 크기
 */
@ConfigurationProperties(prefix = "app.batch")
public record AppBatchProperties(
        int chunkSize,
        int pageSize
) {
}


