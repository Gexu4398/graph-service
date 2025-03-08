package com.singhand.sr.graphservice.testenvironments;

import com.github.javafaker.Faker;
import com.singhand.sr.graphservice.testenvironments.helper.DataHelper;
import com.singhand.sr.graphservice.testenvironments.listener.MyTestExecutionListener;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
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

  @Autowired
  private Driver neo4jDriver;

  // 此处不能用 BeforeEach，否则会清空 CustomTestExecutionListener 创建的数据
  @AfterEach
  @SneakyThrows
  void afterEach() {

    bizEntityManager.clear();
    // 获取数据库连接
    @Cleanup final var conn = bizDataSource.getConnection();
    final var metaData = conn.getMetaData();
    // 获取数据库类型
    final var dbProductName = metaData.getDatabaseProductName().toLowerCase();

    if (dbProductName.contains("h2")) {
      clearH2Database(conn);
    } else if (dbProductName.contains("postgresql")) {
      clearPostgresDatabase(conn);
    } else {
      throw new UnsupportedOperationException("未知数据库连接，无法清理数据: " + dbProductName);
    }

    // 清空 Neo4j 数据
    try (Session session = neo4jDriver.session()) {
      session.run("MATCH (n) DETACH DELETE n");
    }
  }

  /**
   * 清空 H2 数据库（适用于 H2 内存数据库）
   */
  private void clearH2Database(@Nonnull Connection conn) throws SQLException {
    // 禁用外键约束
    conn.createStatement().execute("SET REFERENTIAL_INTEGRITY = FALSE");

    // 获取数据库中的所有表
    final var tables = conn.createStatement().executeQuery("SHOW TABLES");

    // 遍历所有表并执行 TRUNCATE
    while (tables.next()) {
      final var tableName = tables.getString(1);
      conn.createStatement().execute("TRUNCATE TABLE " + tableName);
    }

    // 重新启用外键约束
    conn.createStatement().execute("SET REFERENTIAL_INTEGRITY = TRUE");
  }

  /**
   * 清空 PostgreSQL 数据库
   */
  private void clearPostgresDatabase(@Nonnull Connection conn) throws SQLException {
    // 禁用外键约束
    conn.createStatement().execute("SET session_replication_role = 'replica'");

    // 获取所有用户表
    final var tables = conn.createStatement().executeQuery(
        "SELECT tablename FROM pg_tables WHERE schemaname = 'public'"
    );

    // 遍历所有表并执行 TRUNCATE
    while (tables.next()) {
      final var tableName = tables.getString(1);
      conn.createStatement().execute("TRUNCATE TABLE " + tableName + " CASCADE");
    }

    // 重新启用外键约束
    conn.createStatement().execute("SET session_replication_role = 'origin'");
  }
}
