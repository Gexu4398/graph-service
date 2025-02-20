package com.singhand.sr.graphservice.bizmodel.datasource;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jRepositories(
    basePackages = "com.singhand.sr.graphservice.bizmodel.repository.neo4j",
    transactionManagerRef = "bizNeo4jTransactionManager"
)
@EnableTransactionManagement
public class Neo4jConfig {

  @Bean
  public Driver neo4jDriver(Neo4jProperties neo4jProperties) {

    return GraphDatabase.driver(
        neo4jProperties.getUri(),
        AuthTokens.basic(neo4jProperties.getUsername(), neo4jProperties.getPassword())
    );
  }

  @Bean
  public Neo4jTransactionManager bizNeo4jTransactionManager(Driver driver) {

    return new Neo4jTransactionManager(driver);
  }
}
