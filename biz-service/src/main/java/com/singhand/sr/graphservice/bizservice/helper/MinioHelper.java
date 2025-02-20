package com.singhand.sr.graphservice.bizservice.helper;

import cn.hutool.core.util.StrUtil;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MinioHelper {

  private final MinioClient minioClient;

  private final String bucket;

  @Autowired
  public MinioHelper(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {

    this.minioClient = minioClient;

    this.bucket = bucket;
  }

  @SneakyThrows
  public boolean exists(String url) {

    if (StrUtil.isBlank(url)) {
      return false;
    }

    try {
      minioClient.statObject(StatObjectArgs.builder()
          .bucket(bucket)
          .object(StrUtil.subAfter(url, bucket, true))
          .build());
      return true;
    } catch (ErrorResponseException e) {
      if ("NoSuckKey".equals(e.errorResponse().code())) {
        return false;
      } else {
        throw e;
      }
    }
  }
}
