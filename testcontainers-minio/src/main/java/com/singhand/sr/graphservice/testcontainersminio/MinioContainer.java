package com.singhand.sr.graphservice.testcontainersminio;

import cn.hutool.core.util.RandomUtil;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class MinioContainer extends GenericContainer<MinioContainer> {

  private static final int DEFAULT_PORT = 9000;

  private static final String DEFAULT_IMAGE = "minio/minio";

  private static final String DEFAULT_TAG = "2025.2.18";

  private static final String MINIO_ACCESS_KEY = "MINIO_ROOT_USER";

  private static final String MINIO_SECRET_KEY = "MINIO_ROOT_PASSWORD";

  private static final String DEFAULT_STORAGE_DIRECTORY = "/data";

  public MinioContainer(CredentialsProvider credentials) {

    this(DEFAULT_IMAGE + ":" + DEFAULT_TAG, credentials);
  }

  public MinioContainer(String image, CredentialsProvider credentials) {

    super(image == null ? DEFAULT_IMAGE + ":" + DEFAULT_TAG : image);
    withNetworkAliases("minio-" + RandomUtil.randomString(6));
    addExposedPort(DEFAULT_PORT);
    if (credentials != null) {
      withEnv(MINIO_ACCESS_KEY, credentials.accessKey());
      withEnv(MINIO_SECRET_KEY, credentials.secretKey());
    }
    withCommand("server", DEFAULT_STORAGE_DIRECTORY);
    setWaitStrategy(Wait.forLogMessage("^Documentation.*", 1)
        .withStartupTimeout(Duration.ofMinutes(1)));
  }

  public String getHostAddress() {

    return getHost() + ":" + getMappedPort(DEFAULT_PORT);
  }

  public record CredentialsProvider(String accessKey, String secretKey) {

  }
}