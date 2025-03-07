package com.singhand.sr.graphservice.bizbatchservice.model;

import jakarta.annotation.Nonnull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class MyStepExecutionListener implements StepExecutionListener {

  private final int timeout;

  private long startTime;

  public MyStepExecutionListener(int timeout) {

    this.timeout = timeout;
  }

  @Override
  public void beforeStep(@Nonnull StepExecution stepExecution) {

    startTime = System.currentTimeMillis();
  }

  @Override
  public ExitStatus afterStep(@Nonnull StepExecution stepExecution) {

    if (System.currentTimeMillis() - startTime >= timeout * 1000L) {
      return ExitStatus.FAILED;
    } else {
      return null;
    }
  }
}