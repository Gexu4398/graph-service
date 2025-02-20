package com.singhand.sr.graphservice.bizbatchservice.config;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Slf4j
@EnableBatchProcessing
public class BatchConfig {

  private final PlatformTransactionManager transactionManager;

  private final JobRepository jobRepository;

  private final TaskExecutorProperties taskExecutorProperties;

  @Autowired
  public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      TaskExecutorProperties taskExecutorProperties) {

    this.transactionManager = transactionManager;
    this.jobRepository = jobRepository;
    this.taskExecutorProperties = taskExecutorProperties;
  }

  private JobRepository jobRepository() {

    return jobRepository;
  }

  @Bean
  @SneakyThrows
  public JobLauncher jobLauncher() {

    log.info("使用 TaskExecutorJobLauncher");
    final var jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setTaskExecutor(taskExecutor());
    jobLauncher.setJobRepository(jobRepository());
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  @Bean
  public @NonNull TaskExecutor taskExecutor() {

    log.info("使用 ThreadPoolTaskExecutor");
    log.info("corePoolSize: {}", taskExecutorProperties.getCorePoolSize());
    log.info("maxPoolSize: {}", taskExecutorProperties.getMaxPoolSize());
    log.info("queueCapacity: {}", taskExecutorProperties.getSetQueueCapacity());
    final var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(taskExecutorProperties.getCorePoolSize());
    executor.setMaxPoolSize(taskExecutorProperties.getMaxPoolSize());
    executor.setQueueCapacity(taskExecutorProperties.getSetQueueCapacity());
    executor.setThreadNamePrefix("ThreadPoolTaskExecutor-");
    return executor;
  }
}
