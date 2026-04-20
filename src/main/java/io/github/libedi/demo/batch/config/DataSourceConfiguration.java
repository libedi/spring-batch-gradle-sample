package io.github.libedi.demo.batch.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 멀티 데이터소스와 트랜잭션 매니저 빈을 선언합니다.
 */
@Configuration
@EnableConfigurationProperties(AppBatchProperties.class)
public class DataSourceConfiguration {

    /**
     * Spring Batch 메타데이터용 기본 데이터소스를 생성합니다.
     *
     * @return 기본 배치 데이터소스
     */
    @Bean(name = "batchDataSource")
    @Primary
    @ConfigurationProperties(prefix = "app.datasource.batch")
    public DataSource batchDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * bill 도메인 조회/저장용 데이터소스를 생성합니다.
     *
     * @return bill 데이터소스
     */
    @Bean(name = "billDataSource")
    @ConfigurationProperties(prefix = "app.datasource.bill")
    public DataSource billDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * customer 도메인 조회용 데이터소스를 생성합니다.
     *
     * @return customer 데이터소스
     */
    @Bean(name = "customerDataSource")
    @ConfigurationProperties(prefix = "app.datasource.customer")
    public DataSource customerDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * 기본 배치 데이터소스에 연결된 트랜잭션 매니저를 생성합니다.
     *
     * @param dataSource 기본 배치 데이터소스
     * @return 배치 메타데이터용 트랜잭션 매니저
     */
    @Bean(name = "batchTransactionManager")
    @Primary
    public PlatformTransactionManager batchTransactionManager(
            @Qualifier("batchDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * bill 데이터소스 쓰기용 트랜잭션 매니저를 생성합니다.
     *
     * @param dataSource bill 데이터소스
     * @return bill 도메인 트랜잭션 매니저
     */
    @Bean(name = "billTransactionManager")
    public PlatformTransactionManager billTransactionManager(
            @Qualifier("billDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * customer 데이터소스 조회용 트랜잭션 매니저를 생성합니다.
     *
     * @param dataSource customer 데이터소스
     * @return customer 도메인 트랜잭션 매니저
     */
    @Bean(name = "customerTransactionManager")
    public PlatformTransactionManager customerTransactionManager(
            @Qualifier("customerDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}


