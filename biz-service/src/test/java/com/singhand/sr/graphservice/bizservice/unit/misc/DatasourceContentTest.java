package com.singhand.sr.graphservice.bizservice.unit.misc;

import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceContentRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizservice.BaseTestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@WithMockUser(username = "admin")
public class DatasourceContentTest extends BaseTestEnvironment {

  @Autowired
  private DatasourceRepository datasourceRepository;

  @Autowired
  private DatasourceContentRepository datasourceContentRepository;

  @Autowired
  @Qualifier("bizTransactionManager")
  private PlatformTransactionManager bizTransactionManager;

  @Test
  @SneakyThrows
  void testDatasourceContent() {

    final var datasource = dataHelper.newDatasource(faker.lorem().characters(),
        faker.lorem().characters(),
        faker.lorem().characters(),
        faker.internet().url());

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var managedDatasource = datasourceRepository.findById(datasource.getID()).orElseThrow();

      Assertions.assertNotNull(managedDatasource.getDatasourceContent());
    });
  }
}
