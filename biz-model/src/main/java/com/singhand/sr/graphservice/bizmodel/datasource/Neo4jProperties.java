package com.singhand.sr.graphservice.bizmodel.datasource;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.neo4j")
@Setter
@Getter
@Slf4j
public class Neo4jProperties {

    private String uri;

    private String username;

    private String password;
}
