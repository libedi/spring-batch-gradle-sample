package io.github.libedi.demo.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Batch runtime properties bound from {@code app.batch}.
 *
 * @param chunkSize chunk size for the processing step
 * @param pageSize page size for ID paging reader
 */
@ConfigurationProperties(prefix = "app.batch")
public record AppBatchProperties(
        int chunkSize,
        int pageSize
) {
}
