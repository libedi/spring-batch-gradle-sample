package io.github.libedi.demo.batch.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares MyBatis factories/templates for bill and customer datasources.
 */
@Configuration
public class MybatisConfiguration {

    /**
     * Creates a MyBatis {@link SqlSessionFactory} for the given datasource.
     *
     * @param dataSource target datasource
     * @return configured SqlSessionFactory
     * @throws Exception when factory initialization fails
     */
    private static SqlSessionFactory createSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    /**
     * Bill domain MyBatis configuration.
     */
    @Configuration
    @MapperScan(
            basePackages = "io.github.libedi.demo.batch.mapper.bill",
            sqlSessionTemplateRef = "billSqlSessionTemplate"
    )
    public static class BillConfiguration {

        /**
         * Creates bill domain SqlSessionFactory.
         *
         * @param dataSource bill datasource
         * @return bill SqlSessionFactory
         * @throws Exception when factory initialization fails
         */
        @Bean(name = "billSqlSessionFactory")
        public SqlSessionFactory billSqlSessionFactory(
                @Qualifier("billDataSource") DataSource dataSource
        ) throws Exception {
            return createSqlSessionFactory(dataSource);
        }

        /**
         * Creates bill domain SqlSessionTemplate.
         *
         * @param sqlSessionFactory bill SqlSessionFactory
         * @return bill SqlSessionTemplate
         */
        @Bean(name = "billSqlSessionTemplate")
        public SqlSessionTemplate billSqlSessionTemplate(
                @Qualifier("billSqlSessionFactory") SqlSessionFactory sqlSessionFactory
        ) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }
    }

    /**
     * Customer domain MyBatis configuration.
     */
    @Configuration
    @MapperScan(
            basePackages = "io.github.libedi.demo.batch.mapper.customer",
            sqlSessionTemplateRef = "customerSqlSessionTemplate"
    )
    public static class CustomerConfiguration {

        /**
         * Creates customer domain SqlSessionFactory.
         *
         * @param dataSource customer datasource
         * @return customer SqlSessionFactory
         * @throws Exception when factory initialization fails
         */
        @Bean(name = "customerSqlSessionFactory")
        public SqlSessionFactory customerSqlSessionFactory(
                @Qualifier("customerDataSource") DataSource dataSource
        ) throws Exception {
            return createSqlSessionFactory(dataSource);
        }

        /**
         * Creates customer domain SqlSessionTemplate.
         *
         * @param sqlSessionFactory customer SqlSessionFactory
         * @return customer SqlSessionTemplate
         */
        @Bean(name = "customerSqlSessionTemplate")
        public SqlSessionTemplate customerSqlSessionTemplate(
                @Qualifier("customerSqlSessionFactory") SqlSessionFactory sqlSessionFactory
        ) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }
    }
}
