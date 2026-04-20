package io.github.libedi.demo.batch.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfiguration {

    private static SqlSessionFactory createSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    @Configuration
    @MapperScan(
            basePackages = "io.github.libedi.demo.batch.mapper.bill",
            sqlSessionTemplateRef = "billSqlSessionTemplate"
    )
    public static class BillConfiguration {

        @Bean(name = "billSqlSessionFactory")
        public SqlSessionFactory billSqlSessionFactory(
                @Qualifier("billDataSource") DataSource dataSource
        ) throws Exception {
            return createSqlSessionFactory(dataSource);
        }

        @Bean(name = "billSqlSessionTemplate")
        public SqlSessionTemplate billSqlSessionTemplate(
                @Qualifier("billSqlSessionFactory") SqlSessionFactory sqlSessionFactory
        ) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }
    }

    @Configuration
    @MapperScan(
            basePackages = "io.github.libedi.demo.batch.mapper.customer",
            sqlSessionTemplateRef = "customerSqlSessionTemplate"
    )
    public static class CustomerConfiguration {

        @Bean(name = "customerSqlSessionFactory")
        public SqlSessionFactory customerSqlSessionFactory(
                @Qualifier("customerDataSource") DataSource dataSource
        ) throws Exception {
            return createSqlSessionFactory(dataSource);
        }

        @Bean(name = "customerSqlSessionTemplate")
        public SqlSessionTemplate customerSqlSessionTemplate(
                @Qualifier("customerSqlSessionFactory") SqlSessionFactory sqlSessionFactory
        ) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }
    }
}
