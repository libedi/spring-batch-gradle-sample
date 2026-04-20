package io.github.libedi.demo.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 배치 컴포넌트에서 사용하는 Jackson 빈을 선언합니다.
 */
@Configuration
public class JacksonConfiguration {

    /**
     * NDJSON 직렬화를 위한 {@link ObjectMapper} 빈을 생성합니다.
     *
     * @return 공용 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}


