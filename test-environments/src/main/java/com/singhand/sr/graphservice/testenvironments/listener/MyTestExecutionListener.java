package com.singhand.sr.graphservice.testenvironments.listener;

import com.singhand.sr.graphservice.testenvironments.helper.DataHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
public class MyTestExecutionListener implements TestExecutionListener, Ordered {

  @Autowired
  private DataHelper dataHelper;

  @Autowired
  @Qualifier("bizTransactionManager")
  private PlatformTransactionManager bizTransactionManager;

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

    }
  }
}
