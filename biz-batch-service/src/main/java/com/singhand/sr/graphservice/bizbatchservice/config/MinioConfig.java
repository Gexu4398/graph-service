package com.singhand.sr.graphservice.bizbatchservice.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@Configuration
public class MinioConfig {

  private String endpoint;

  private String username;

  private String password;

  private String bucket;

  private String bucketNotificationTopic;

  @Bean
  public MinioClient minioClient() {

    return MinioClient.builder()
        .endpoint(endpoint)
        .credentials(username, password)
        .build();
  }
}
