package com.singhand.sr.graphservice.testcontainerselasticsearch;

import cn.hutool.core.util.RandomUtil;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

  private static final int DEFAULT_PORT = 9200;

  private static final String DEFAULT_IMAGE = "elasticsearch";

  private static final String DEFAULT_TAG = "8.15.3";

  public ElasticsearchContainer() {

    this(DEFAULT_IMAGE + ":" + DEFAULT_TAG);
  }

  public ElasticsearchContainer(String image) {

    super(image == null ? DEFAULT_IMAGE + ":" + DEFAULT_TAG : image);
    withNetworkAliases("elasticsearch-" + RandomUtil.randomString(6));
    withEnv("ES_JAVA_OPTS", "-Xms8192m -Xmx8192m");
    withEnv("cluster.name", "elasticsearch");
    withEnv("discovery.type", "single-node");
    withEnv("xpack.security.enabled", "false");
    withClasspathResourceMapping(
        "./init-elasticsearch/plugins",
        "/usr/share/elasticsearch/plugins",
        BindMode.READ_ONLY
    );
    addExposedPorts(DEFAULT_PORT, 9300);
  }

  public String getHostAddress() {

    return getHost() + ":" + getMappedPort(DEFAULT_PORT);
  }
}
