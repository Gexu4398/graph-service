package com.singhand.sr.graphservice.bizservice;

import com.singhand.sr.graphservice.bizservice.config.IndexerConfig;
import com.singhand.sr.graphservice.bizservice.config.MinioConfig;
import com.singhand.sr.graphservice.bizservice.demo.DemoService;
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

    final var context = SpringApplication.run(BizServiceApplication.class, args);

    if ("always".equals(context.getEnvironment().getProperty("app.reset-with-demo-data"))) {

      log.warn("正在初始化测试数据...");

      ((DemoService) context.getBean("ontologyDemoService")).run();

      log.warn("测试数据初始化完成！");
    }
  }

  @Bean
  @SneakyThrows
  public ApplicationRunner buildIndex(IndexerConfig indexerConfig) {

    return application -> indexerConfig.indexPersistedData();
  }
}