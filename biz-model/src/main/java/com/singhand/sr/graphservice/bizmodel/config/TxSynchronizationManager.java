package com.singhand.sr.graphservice.bizmodel.config;

import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TxSynchronizationManager {

  public void executeAfterCommit(Runnable runnable) {

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {

            @Override
            public void afterCommit() {

              runnable.run();
            }
          }
      );
    } else {
      // If no transaction active, execute directly
      runnable.run();
    }
  }

  public <T> void executeAfterCommit(Consumer<T> consumer, T param) {

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {

            @Override
            public void afterCommit() {

              consumer.accept(param);
            }
          }
      );
    } else {
      // If no transaction active, execute directly
      consumer.accept(param);
    }
  }
}
