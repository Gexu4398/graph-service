package com.singhand.sr.graphservice.testenvironments;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.KeycloakRole;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.UserEntity;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.UserEntityRepository;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import io.minio.MinioClient;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class UnitTestEnvironment extends TestEnvironment {

  @MockitoBean
  private UserEntityRepository userEntityRepository;

  @MockitoBean
  private KeycloakService keycloakService;

  @MockitoBean
  private MinioClient minioClient;

  @DynamicPropertySource
  @SneakyThrows
  static void bindProperties(DynamicPropertyRegistry registry) {
    // 单元测试不依赖于外部环境，所以此处禁用 Zookeeper
    registry.add("spring.cloud.zookeeper.enabled", () -> false);
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
