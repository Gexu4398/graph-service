package com.singhand.sr.graphservice.bizbatchservice;

import com.singhand.sr.graphservice.bizbatchservice.service.PostLaunchService;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.singhand.sr.graphservice")
@Slf4j
@EnableFeignClients
@EnableScheduling
public class BizBatchServiceApplication {

  public static void main(String[] args) {

    final var context = SpringApplication.run(BizBatchServiceApplication.class, args);
    log.debug(">>> Datasource instance: " + context.getBean("dataSource"));
    log.debug(">>> TransactionManager instance: " + context.getBean("transactionManager"));
    postLaunch(context);
  }

  private static void postLaunch(@Nonnull ApplicationContext context) {

    final var postLaunchService = context.getBean(PostLaunchService.class);
    postLaunchService.abortRunningJobs();
    postLaunchService.removeUnusedJobInstances();
  }
}
