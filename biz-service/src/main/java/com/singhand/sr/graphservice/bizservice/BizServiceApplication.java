package com.singhand.sr.graphservice.bizservice;

import com.singhand.sr.graphservice.bizservice.config.IndexerConfig;
import com.singhand.sr.graphservice.bizservice.config.MinioConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.singhand.sr.graphservice")
@ConfigurationPropertiesScan(basePackageClasses = {MinioConfig.class})
@EnableFeignClients
@EnableScheduling
@EnableCaching
@Slf4j
public class BizServiceApplication {

  public static void main(String[] args) {

    SpringApplication.run(BizServiceApplication.class, args);
  }

  @Bean
  @SneakyThrows
  public ApplicationRunner buildIndex(IndexerConfig indexerConfig) {

    return application -> indexerConfig.indexPersistedData();
  }
}