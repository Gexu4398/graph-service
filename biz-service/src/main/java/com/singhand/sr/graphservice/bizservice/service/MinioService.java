package com.singhand.sr.graphservice.bizservice.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.RandomUtil;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MinioService {

  private final MinioClient minioClient;

  private final String bucket;

  private final Pattern objectNamePattern = Pattern.compile(
      "^/[^/]+/(?<path>[^/]+)/(?<date>[^/]+)/(?<name>[^/]+)$");

  @Autowired
  public MinioService(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {

    this.minioClient = minioClient;
    this.bucket = bucket;
  }

  @SneakyThrows
  public String putPreSignedURL(String name, String directory, Map<String, String> metadata,
      boolean keepName) {

    final var salt = keepName ? DateUtil.now().replace(":", "-") : RandomUtil.randomString(32);
    final var ext = FileNameUtil.extName(name);

    final var filename = String.format("%s.%s",
        keepName ? FileNameUtil.mainName(name) + "_" + salt : salt, ext);

    return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
        .extraHeaders(metadata)
        .method(Method.PUT)
        .bucket(bucket)
        .object(
            String.format("%s/%s/%s", directory, DateUtil.formatDate(DateUtil.date()), filename))
        .expiry(24, TimeUnit.HOURS)
        .build());
  }

  @SneakyThrows
  public String getPreSignedURL(String name) {

    return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(bucket)
        .object(name)
        .expiry(24, TimeUnit.HOURS)
        .build());
  }

  @SneakyThrows
  public void putObject(String text, String object) {

    @Cleanup final var inputStream = IoUtil.toUtf8Stream(text);
    minioClient.putObject(PutObjectArgs.builder()
        .bucket(bucket)
        .object(object)
        .stream(inputStream, -1, 10485760)
        .build());
  }

  public String getObjectFromKey(@Nonnull String key) {

    if (key.contains("https")) {
      return "";
    }
    final var matcher = objectNamePattern.matcher(key);
    if (!matcher.matches()) {
      return "";
    }
    final var path = matcher.group("path");
    final var date = matcher.group("date");
    final var filename = matcher.group("name");
    return String.format("%s/%s/%s", path, date, filename);
  }
}
