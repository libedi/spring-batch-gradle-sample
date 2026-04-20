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
 * bill/customer 데이터소스용 MyBatis 팩토리와 템플릿을 선언합니다.
 */
@Configuration
public class MybatisConfiguration {

    /**
     * 지정한 데이터소스용 MyBatis {@link SqlSessionFactory}를 생성합니다.
     *
     * @param dataSource 대상 데이터소스
     * @return 구성된 SqlSessionFactory
     * @throws Exception 팩토리 초기화 실패 시
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
     * bill 도메인 MyBatis 설정입니다.
     */
    @Configuration
    @MapperScan(
            basePackages = "io.github.libedi.demo.batch.mapper.bill",
            sqlSessionTemplateRef = "billSqlSessionTemplate"
    )
    public static class BillConfiguration {

        /**
         * bill 도메인 SqlSessionFactory를 생성합니다.
         *
         * @param dataSource bill 데이터소스
         * @return bill SqlSessionFactory
         * @throws Exception 팩토리 초기화 실패 시
         */
        @Bean(name = "billSqlSessionFactory")
        public SqlSessionFactory billSqlSessionFactory(
                @Qualifier("billDataSource") DataSource dataSource
        ) throws Exception {
            return createSqlSessionFactory(dataSource);
        }

        /**
         * bill 도메인 SqlSessionTemplate를 생성합니다.
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
     * customer 도메인 MyBatis 설정입니다.
     */
    @Configuration
    @MapperScan(
            basePackages = "io.github.libedi.demo.batch.mapper.customer",
            sqlSessionTemplateRef = "customerSqlSessionTemplate"
    )
    public static class CustomerConfiguration {

        /**
         * customer 도메인 SqlSessionFactory를 생성합니다.
         *
         * @param dataSource customer 데이터소스
         * @return customer SqlSessionFactory
         * @throws Exception 팩토리 초기화 실패 시
         */
        @Bean(name = "customerSqlSessionFactory")
        public SqlSessionFactory customerSqlSessionFactory(
                @Qualifier("customerDataSource") DataSource dataSource
        ) throws Exception {
            return createSqlSessionFactory(dataSource);
        }

        /**
         * customer 도메인 SqlSessionTemplate를 생성합니다.
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


