package com.singhand.sr.graphservice.bizbatchservice.datasource;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionManager;

@Configuration
public class Batch {

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSourceProperties batchDataSourceProperties() {

    return new DataSourceProperties();
  }

  @Bean
  public DataSource dataSource(DataSourceProperties batchDataSourceProperties) {

    return batchDataSourceProperties
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Bean
  public TransactionManager transactionManager(DataSource dataSource) {

    return new JdbcTransactionManager(dataSource);
  }
}
