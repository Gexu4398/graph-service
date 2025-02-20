package com.singhand.sr.graphservice.testenvironments;

import com.github.javafaker.Faker;
import com.singhand.sr.graphservice.testenvironments.helper.DataHelper;
import com.singhand.sr.graphservice.testenvironments.listener.MyTestExecutionListener;
import jakarta.persistence.EntityManager;
import java.util.Locale;
import javax.sql.DataSource;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API 测试基类。 该类对于 Keycloak 和 Minio 系统的相关 Bean 进行了 Mock 处理。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=test",
    "app.show-sql=true",
    "logging.level.liquibase=debug",
})
@Slf4j
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@TestInstance(Lifecycle.PER_CLASS)
// 此处要设置 mergeMode，否则回替换调原来的 listeners，会导致部分测试失败
// 参考文献 https://www.baeldung.com/spring-testexecutionlistener
@TestExecutionListeners(value = MyTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@Rollback(false)
abstract class TestEnvironment {

  protected Faker faker = new Faker(Locale.ENGLISH);

  @Autowired
  protected DataHelper dataHelper;

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  @Qualifier("bizEntityManager")
  protected EntityManager bizEntityManager;

  @Autowired
  @Qualifier("keycloakEntityManager")
  protected EntityManager keycloakEntityManager;

  @Autowired
  @Qualifier("bizDataSource")
  private DataSource bizDataSource;

  // 此处不能用 BeforeEach，否则会清空 CustomTestExecutionListener 创建的数据
  @AfterEach
  @SneakyThrows
  void afterEach() {

    bizEntityManager.clear();
    @Cleanup final var conn = bizDataSource.getConnection();
    // 禁用约束
    conn.createStatement().execute("SET REFERENTIAL_INTEGRITY = FALSE");
    // 罗列表对象
    final var tables = conn.createStatement().executeQuery("show tables");
    // 删除表数据
    while (tables.next()) {
      final var tableName = String.format("%s.%s", tables.getString(2), tables.getString(1));
      conn.createStatement().execute("truncate table " + tableName);
    }
    // 启用约束
    conn.createStatement().execute("SET REFERENTIAL_INTEGRITY = TRUE");
  }
}
