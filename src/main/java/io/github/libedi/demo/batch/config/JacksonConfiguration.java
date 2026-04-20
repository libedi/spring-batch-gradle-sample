package io.github.libedi.demo.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares Jackson related beans used by batch components.
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Creates an {@link ObjectMapper} bean for NDJSON serialization.
     *
     * @return shared ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}
