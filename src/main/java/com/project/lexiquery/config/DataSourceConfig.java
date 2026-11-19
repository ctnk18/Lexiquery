package com.project.lexiquery.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Two databases:
 *  - app DB    -> Hibernate/JPA (users, history)       [primary]
 *  - target DB -> plain JDBC with a READ-ONLY account  (runs AI-generated SQL)
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource")
    public DataSource appDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean("targetDataSource")
    public DataSource targetDataSource(LexiQueryProperties p) {
        HikariDataSource ds = DataSourceBuilder.create().type(HikariDataSource.class)
                .url(p.target().url()).username(p.target().username()).password(p.target().password()).build();
        ds.setReadOnly(true);
        ds.setMaximumPoolSize(5);
        return ds;
    }

    @Bean("targetJdbcTemplate")
    public JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource ds, LexiQueryProperties p) {
        JdbcTemplate jt = new JdbcTemplate(ds);
        jt.setQueryTimeout(p.limits().timeoutSeconds());
        jt.setMaxRows(p.limits().maxRows());
        return jt;
    }
}
