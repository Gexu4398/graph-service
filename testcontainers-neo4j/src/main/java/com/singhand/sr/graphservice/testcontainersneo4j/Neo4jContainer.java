package com.singhand.sr.graphservice.testcontainersneo4j;

import cn.hutool.core.util.RandomUtil;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

public class Neo4jContainer extends GenericContainer<Neo4jContainer> {

  private static final int DEFAULT_PORT = 7687;

  private static final String DEFAULT_IMAGE = "neo4j";

  private static final String DEFAULT_TAG = "5.26.2";

  public Neo4jContainer() {

    this(DEFAULT_IMAGE + ":" + DEFAULT_TAG);
  }

  public Neo4jContainer(String image) {

    super(image == null ? DEFAULT_IMAGE + ":" + DEFAULT_TAG : image);
    withNetworkAliases("elasticsearch-" + RandomUtil.randomString(6));
    withEnv("NEO4J_AUTH", "neo4j/neo4jexample");
    withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*");
    withEnv("NEO4J_PLUGINS", "[\"apoc\"]");
    withClasspathResourceMapping(
        "./init-neo4j/plugins",
        "/plugins",
        BindMode.READ_ONLY
    );
    addExposedPorts(DEFAULT_PORT, 7474);
  }

  public String getHostAddress() {

    return String.format("bolt://%s:%s", getHost(), getMappedPort(DEFAULT_PORT));
  }
}
