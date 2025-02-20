package com.singhand.sr.graphservice.bizbatchservice.service;

import io.minio.MinioClient;
import jakarta.annotation.Nonnull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {

  private final JobLauncher jobLauncher;

  private final String bucket;

  private final MinioClient minioClient;

  @Autowired
  public KafkaConsumerService(JobLauncher jobLauncher, @Value("${minio.bucket}") String bucket,
      MinioClient minioClient) {

    this.jobLauncher = jobLauncher;
    this.bucket = bucket;
    this.minioClient = minioClient;
  }

  @KafkaListener(topics = "minio_bucket_notification", groupId = "minio", containerFactory = "kafkaListenerContainerFactory")
  @SneakyThrows
  public void minioBucketNotification(ConsumerRecord<?, ?> consumerRecord,
      @Nonnull Acknowledgment acknowledgment) {

    log.info("minio_bucket_notification: {}", consumerRecord);

    acknowledgment.acknowledge();
  }
}
