package com.singhand.sr.graphservice.testenvironments;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import jakarta.annotation.Nonnull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@Testcontainers
public abstract class KeycloakIntegrationTestEnvironment extends TestEnvironment {

  private final static String PGSQL_ROOT_USER = "root";

  private final static String PGSQL_ROOT_PASSWORD = "example";

  public final static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
      DockerImageName.parse("postgres:alpine3.21"))
      .withUsername(PGSQL_ROOT_USER)
      .withPassword(PGSQL_ROOT_PASSWORD)
      .withClasspathResourceMapping("databases.sql", "/docker-entrypoint-initdb.d/databases.sql",
          BindMode.READ_ONLY);

  public final static GenericContainer<?> keycloak = new GenericContainer<>(
      DockerImageName.parse("keycloak/keycloak:26.1.4"))
      .withEnv("BASE_URL", "http://localhost/")
      .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
      .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
      .withEnv("KC_DB_URL", "jdbc:postgresql://postgres:5432/keycloak")
      .withEnv("KC_DB", "postgres")
      .withEnv("KC_DB_USERNAME", PGSQL_ROOT_USER)
      .withEnv("KC_DB_PASSWORD", PGSQL_ROOT_PASSWORD)
      .withEnv("KC_HOSTNAME_STRICT", "false")
      .withEnv("KC_PROXY_HEADERS", "xforwarded")
      .withEnv("KC_HTTP_RELATIVE_PATH", "/auth")
      .withEnv("KC_HTTP_ENABLED", "true")
      .withCommand("start --http-enabled=true --import-realm")
      .withClasspathResourceMapping("realm-export.json",
          "/opt/keycloak/data/import/realm.json", BindMode.READ_ONLY)
      .withExposedPorts(8080)
      .withLogConsumer(new Slf4jLogConsumer(log))
      .dependsOn(postgres);

  @Autowired
  private KeycloakService keycloakService;

  @DynamicPropertySource
  @SneakyThrows
  static void bindProperties(@Nonnull DynamicPropertyRegistry registry) {

    final var network = Network.newNetwork();

    postgres.withNetwork(network).withNetworkAliases("postgres").start();
    final var graphJdbcUrl = postgres.getJdbcUrl().replace("test", "graph");
    final var keycloakJdbcUrl = postgres.getJdbcUrl().replace("test", "keycloak");
    keycloak.withNetwork(network).withNetworkAliases("keycloak").start();
    registry.add("keycloak.auth-server-url", () -> String.format("http://%s:%d/auth",
        keycloak.getHost(), keycloak.getMappedPort(8080)));
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> String.format("http://%s:%s/auth/realms/console-app/protocol/openid-connect/certs",
            keycloak.getHost(), keycloak.getMappedPort(8080)));

    // 业务数据源配置
    registry.add("app.datasource.biz.url", () -> graphJdbcUrl);
    registry.add("app.datasource.biz.jdbcUrl", () -> graphJdbcUrl);
    registry.add("app.datasource.biz.username", () -> "root");
    registry.add("app.datasource.biz.password", () -> "example");
    registry.add("app.datasource.biz.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("app.datasource.biz.dialect",
        () -> "org.hibernate.dialect.PostgreSQLDialect");

    // Keycloak 数据源配置
    registry.add("app.datasource.keycloak.url", () -> keycloakJdbcUrl);
    registry.add("app.datasource.keycloak.jdbcUrl", () -> keycloakJdbcUrl);
    registry.add("app.datasource.keycloak.username", () -> "root");
    registry.add("app.datasource.keycloak.password", () -> "example");
    registry.add("app.datasource.keycloak.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("app.datasource.keycloak.dialect",
        () -> "org.hibernate.dialect.PostgreSQLDialect");
  }

  @AfterEach
  @Override
  void afterEach() {

    super.afterEach();

    keycloakService.getUsersResource().list().forEach(it -> {
      if (!it.getUsername().equals("admin") && !it.getUsername().startsWith("reserved_")) {
        keycloakService.getUserResourceById(it.getId()).remove();
      }
    });

    keycloakService.getClientResource().roles().list().forEach(it -> {
      if (!it.getName().equals("超级管理员")) {
        keycloakService.getClientRoleResource(it.getName()).remove();
      }
    });

    keycloakService.getGroupsResource().groups().forEach(it ->
        keycloakService.getGroupResource(it.getId()).remove());
  }
}
