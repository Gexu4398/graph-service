package com.singhand.sr.graphservice.bizservice.config;

import io.minio.GetBucketNotificationArgs;
import io.minio.MinioClient;
import io.minio.SetBucketNotificationArgs;
import io.minio.messages.EventType;
import io.minio.messages.NotificationConfiguration;
import io.minio.messages.QueueConfiguration;
import java.util.LinkedList;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@Configuration
@Slf4j
public class MinioConfig {

  private final static String SQS = "arn:minio:sqs::GRAPH:kafka";

  private String endpoint;

  private String username;

  private String password;

  private String bucket;

  private String bucketNotificationTopic;

  @Bean
  public MinioClient minioClient() {

    log.info("初始化 MinioClient ...");

    final var client = MinioClient.builder()
        .endpoint(this.endpoint)
        .credentials(username, password)
        .build();

    if (!existsBucketNotification(client)) {
      log.info("不存在 SQS 设置，正在自动设置 ...");
      setBucketNotification(client);
      log.info("Minio SQS 自动设置完成");
    }

    log.info("MinioClient 初始化完成");
    return client;
  }

  @SneakyThrows
  private boolean existsBucketNotification(MinioClient minioClient) {

    final var config = minioClient.getBucketNotification(
        GetBucketNotificationArgs.builder().bucket(bucket).build());

    return config.queueConfigurationList()
        .stream()
        .anyMatch(it -> SQS.equals(it.queue()));
  }

  @SneakyThrows
  private void setBucketNotification(MinioClient minioClient) {

    final var eventList = new LinkedList<EventType>();
    eventList.add(EventType.OBJECT_CREATED_PUT);

    final var queueConfiguration = new QueueConfiguration();
    queueConfiguration.setQueue(SQS);
    queueConfiguration.setEvents(eventList);

    final var queueConfigurationList = new LinkedList<QueueConfiguration>();
    queueConfigurationList.add(queueConfiguration);

    final var config = new NotificationConfiguration();
    config.setQueueConfigurationList(queueConfigurationList);

    minioClient.setBucketNotification(
        SetBucketNotificationArgs.builder().bucket(bucket).config(config).build());
  }
}
