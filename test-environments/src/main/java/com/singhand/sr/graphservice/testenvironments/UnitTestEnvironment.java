package com.singhand.sr.graphservice.testenvironments;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.KeycloakRole;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.UserEntity;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.UserEntityRepository;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import io.minio.MinioClient;
import java.util.Optional;
import java.util.Set;
import jakarta.annotation.Nonnull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class UnitTestEnvironment extends TestEnvironment {

  @MockitoBean
  private UserEntityRepository userEntityRepository;

  @MockitoBean
  private KeycloakService keycloakService;

  @MockitoBean
  private MinioClient minioClient;

  public final static Neo4jContainer<?> neo4j = new Neo4jContainer<>(
      DockerImageName.parse("neo4j:5.26.2"))
      .withEnv("NEO4J_AUTH", "neo4j/neo4jexample")
      .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*")
      .withEnv("NEO4J_PLUGINS", "[\"apoc\"]")
      .withClasspathResourceMapping("./init-neo4j/plugins", "/plugins", BindMode.READ_ONLY)
      .withExposedPorts(7474, 7687);

  @DynamicPropertySource
  @SneakyThrows
  static void bindProperties(@Nonnull DynamicPropertyRegistry registry) {

    final var network = Network.newNetwork();

    neo4j.withNetwork(network).withNetworkAliases("neo4j").start();

    // 单元测试不依赖于外部环境，所以此处禁用 Zookeeper
    registry.add("spring.cloud.zookeeper.enabled", () -> false);
    final var neo4jHost = String.format("bolt://%s:%s", neo4j.getHost(), neo4j.getMappedPort(7687));
    registry.add("app.neo4j.uri", () -> neo4jHost);
  }

  @BeforeAll
  void beforeAll() {

    final var userEntity = new UserEntity();
    userEntity.setId("admin");
    userEntity.setUsername("admin");
    userEntity.setFirstName("admin");
    userEntity.setRoles(Set.of(KeycloakRole.builder().name("超级管理员").clientRole(true).build()));
    Mockito.when(keycloakService.getRealm()).thenReturn("console-app");
    Mockito.when(userEntityRepository.findByUsernameAndRealmId("admin", keycloakService.getRealm()))
        .thenReturn(Optional.of(userEntity));
  }
}
