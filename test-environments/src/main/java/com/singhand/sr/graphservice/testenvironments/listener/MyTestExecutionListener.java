package com.singhand.sr.graphservice.testenvironments.listener;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.testenvironments.helper.DataHelper;
import com.singhand.sr.graphservice.testenvironments.mock.MockDataSource;
import com.singhand.sr.graphservice.testenvironments.mock.MockDataSources;
import com.singhand.sr.graphservice.testenvironments.mock.MockOntologies;
import com.singhand.sr.graphservice.testenvironments.mock.MockOntology;
import com.singhand.sr.graphservice.testenvironments.mock.MockRelationModel;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class MyTestExecutionListener implements TestExecutionListener, Ordered {

  @Autowired
  private DataHelper dataHelper;

  @Autowired
  @Qualifier("bizTransactionManager")
  private PlatformTransactionManager bizTransactionManager;

  @Autowired
  private DatasourceRepository datasourceRepository;

  @Override
  public void beforeTestClass(TestContext testContext) {

    testContext.getApplicationContext()
        .getAutowireCapableBeanFactory()
        .autowireBean(this);
  }

  @Override
  public int getOrder() {

    return Integer.MAX_VALUE;
  }

  @Override
  public void beforeTestMethod(@NonNull TestContext testContext) throws Exception {

    TestExecutionListener.super.beforeTestMethod(testContext);

    final var annotations = testContext.getTestMethod().getAnnotations();
    for (final var annotation : annotations) {
      if (annotation instanceof MockOntology mockOntology) {
        newOntology(mockOntology);
      } else if (annotation instanceof MockOntologies mockOntologies) {
        newOntologies(mockOntologies);
      } else if (annotation instanceof MockRelationModel mockRelationModel) {
        newRelationModel(mockRelationModel);
      } else if (annotation instanceof MockDataSource mockDataSource) {
        getOrCreateDataSource(mockDataSource);
      } else if (annotation instanceof MockDataSources mockDataSources) {
        newDataSources(mockDataSources);
      }
    }
  }

  private void newDataSources(@Nonnull MockDataSources mockDataSources) {

    for (final var mockDataSource : mockDataSources.value()) {
      getOrCreateDataSource(mockDataSource);
    }
  }

  private Datasource getOrCreateDataSource(@Nonnull MockDataSource mockDataSource) {

    return datasourceRepository.findFirstByTitle(mockDataSource.title())
        .orElseGet(() -> dataHelper.newDatasource(
            mockDataSource.title(),
            mockDataSource.sourceType(),
            mockDataSource.contentType(),
            ""));
  }

  void newOntologies(@Nonnull MockOntologies mockOntologies) {

    for (final var mockOntology : mockOntologies.value()) {
      newOntology(mockOntology);
    }
  }

  void newRelationModel(MockRelationModel mockRelationModel) {

    new TransactionTemplate(bizTransactionManager)
        .executeWithoutResult(transactionStatus ->
            dataHelper.newRelationModel(mockRelationModel.name()));
  }

  void newOntology(MockOntology mockOntology) {

    // Listener 的回调方法不是被 Proxy 调用的，因此即使加 @Transactional 也是没有用的，需要手工启动事务。
    new TransactionTemplate(bizTransactionManager).executeWithoutResult(transactionStatus -> {
      final var ontology = dataHelper.newOntology(mockOntology.name(), null);

      for (final var property : mockOntology.properties()) {
        dataHelper.newOntologyProperty(ontology, property.name(), property.type());
      }
    });
  }
}
