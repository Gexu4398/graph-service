package com.singhand.sr.graphservice.bizbatchservice.service;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PostLaunchService {

  private final JobRepository jobRepository;

  private final JobExplorer jobExplorer;

  private final JobOperator jobOperator;

  @Autowired
  public PostLaunchService(JobRepository jobRepository, JobExplorer jobExplorer,
      JobOperator jobOperator) {

    this.jobRepository = jobRepository;

    this.jobExplorer = jobExplorer;

    this.jobOperator = jobOperator;
  }

  public void removeUnusedJobInstances() {
    // delete all job instances that are not running
    jobRepository.getJobNames().forEach(
        jobName -> jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE)
            .forEach(jobInstance -> {
                  final var isRunning = jobExplorer.getJobExecutions(jobInstance).stream()
                      .anyMatch(JobExecution::isRunning);
                  if (!isRunning) {
                    log.info("删除 JobInstance {}-{}", jobInstance.getJobName(),
                        jobInstance.getInstanceId());
                    deleteJobExecutions(jobInstance);
                    jobRepository.deleteJobInstance(jobInstance);
                    return;
                  }
                  log.info("JobInstance {}-{} 正在执行，不可删除！", jobInstance.getJobName(),
                      jobInstance.getInstanceId());
                }
            ));
  }

  private void deleteJobExecutions(JobInstance jobInstance) {

    jobExplorer.getJobExecutions(jobInstance).stream()
        .filter(jobExecution -> !jobExecution.isRunning())
        .forEach(jobExecution -> {
          jobExecution.getStepExecutions().forEach(stepExecution -> {
            log.info("删除 StepExecution {}-{}", stepExecution.getStepName(),
                stepExecution.getId());
            jobRepository.deleteStepExecution(stepExecution);
          });
          log.info("删除 JobExecution {}-{}", jobExecution.getJobInstance().getJobName(),
              jobExecution.getId());
          jobRepository.deleteJobExecution(jobExecution);
        });
  }

  public void abortRunningJobs() {

    log.info("启动时检查未完成的任务……");
    jobExplorer.getJobNames().forEach(jobName -> {
      log.info("检查任务 {} ...", jobName);
      try {
        jobOperator.getRunningExecutions(jobName).forEach(execution -> {
          log.info("停止中 ... {}", execution);
          Optional.ofNullable(jobExplorer.getJobExecution(execution)).ifPresent(jobExecution -> {
            jobExecution.getStepExecutions().forEach(stepExecution -> {
              log.info("停止 Step ...");
              stepExecution.setStatus(BatchStatus.ABANDONED);
              stepExecution.setExitStatus(ExitStatus.FAILED);
              jobRepository.update(stepExecution);
            });
            jobExecution.setStatus(BatchStatus.ABANDONED);
            jobExecution.setExitStatus(ExitStatus.FAILED);
            jobRepository.update(jobExecution);
            log.info("已停止！");
          });
        });
      } catch (NoSuchJobException ignored) {
      }
    });
  }
}
