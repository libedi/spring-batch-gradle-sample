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
 * Declares multi-datasource and transaction manager beans.
 */
@Configuration
@EnableConfigurationProperties(AppBatchProperties.class)
public class DataSourceConfiguration {

    /**
     * Creates the primary datasource used by Spring Batch metadata.
     *
     * @return primary batch datasource
     */
    @Bean(name = "batchDataSource")
    @Primary
    @ConfigurationProperties(prefix = "app.datasource.batch")
    public DataSource batchDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * Creates the datasource for bill domain read/write operations.
     *
     * @return bill datasource
     */
    @Bean(name = "billDataSource")
    @ConfigurationProperties(prefix = "app.datasource.bill")
    public DataSource billDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * Creates the datasource for customer domain read operations.
     *
     * @return customer datasource
     */
    @Bean(name = "customerDataSource")
    @ConfigurationProperties(prefix = "app.datasource.customer")
    public DataSource customerDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * Creates the transaction manager bound to the primary batch datasource.
     *
     * @param dataSource primary batch datasource
     * @return transaction manager for batch metadata
     */
    @Bean(name = "batchTransactionManager")
    @Primary
    public PlatformTransactionManager batchTransactionManager(
            @Qualifier("batchDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Creates the transaction manager for bill datasource writes.
     *
     * @param dataSource bill datasource
     * @return transaction manager for bill domain
     */
    @Bean(name = "billTransactionManager")
    public PlatformTransactionManager billTransactionManager(
            @Qualifier("billDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Creates the transaction manager for customer datasource reads.
     *
     * @param dataSource customer datasource
     * @return transaction manager for customer domain
     */
    @Bean(name = "customerTransactionManager")
    public PlatformTransactionManager customerTransactionManager(
            @Qualifier("customerDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}
