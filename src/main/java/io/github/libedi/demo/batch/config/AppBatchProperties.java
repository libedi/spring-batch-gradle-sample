package io.github.libedi.demo.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.batch")
public record AppBatchProperties(
        int chunkSize,
        int pageSize
) {
}
