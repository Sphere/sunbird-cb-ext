package org.sunbird.core.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dedicated, self-contained PostgreSQL connection used ONLY by {@code CertificateController}
 * (the {@code /v1/certificate/status} and {@code /v1/certificate/download} endpoints, which
 * read the {@code user_program_completion} table).
 *
 * <p>This deliberately does <b>not</b> declare a {@link DataSource} or
 * {@code DataSourceProperties} bean. Declaring one would make Spring Boot's
 * {@code DataSourceAutoConfiguration} back off and disturb the existing
 * {@code spring.datasource.*} / JPA / Cassandra wiring. Instead it builds a private Hikari
 * pool <i>inside</i> the {@code JdbcTemplate} bean, so the rest of the application is left
 * completely untouched.</p>
 *
 * <p>Inject it with {@code @Qualifier("certificateJdbcTemplate")}.</p>
 */
@Configuration
public class CertificateDataSourceConfig {

    @Bean(name = "certificateJdbcTemplate")
    public JdbcTemplate certificateJdbcTemplate(
            @Value("${certificate.datasource.url}") String url,
            @Value("${certificate.datasource.username}") String username,
            @Value("${certificate.datasource.password}") String password,
            @Value("${certificate.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName) {
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
        return new JdbcTemplate(dataSource);
    }
}