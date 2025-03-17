package com.singhand.sr.graphservice.bizbatchservice.controller;

import cn.hutool.core.util.ObjUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.ImportVertexRequest;
import com.singhand.sr.graphservice.bizmodel.model.reponse.OperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("batch-job")
@Tag(name = "批量处理")
@Slf4j
public class BatchJobController {

  private final JobExplorer jobExplorer;

  private final JobRepository jobRepository;

  private final JobLauncher jobLauncher;

  private final Job importDatasourceJob;

  private final Job importVertexJob;

  @Autowired
  public BatchJobController(JobExplorer jobExplorer, JobRepository jobRepository,
      JobLauncher jobLauncher, Job importDatasourceJob, Job importVertexJob) {

    this.jobExplorer = jobExplorer;
    this.jobRepository = jobRepository;
    this.jobLauncher = jobLauncher;
    this.importDatasourceJob = importDatasourceJob;
    this.importVertexJob = importVertexJob;
  }

  @GetMapping("{id}")
  public OperationResponse getExecutionStatus(@PathVariable Long id) {

    final var jobExecution = jobExplorer.getJobExecution(id);
    return jobExecution2OperationResponse(Optional.ofNullable(jobExecution)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  @GetMapping("{name}/uuid/{uuid}")
  public OperationResponse getExecutionStatusByJobNameAndUuid(@PathVariable String name,
      @PathVariable String uuid) {

    final var jobExecution = jobRepository.getLastJobExecution(name,
        new JobParametersBuilder().addString("instance_id", uuid, true).toJobParameters());
    if (jobExecution == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到任务 " + uuid);
    }
    return jobExecution2OperationResponse(jobExecution);
  }

  @Operation(summary = "后台任务查询")
  @GetMapping("latest")
  @SneakyThrows
  public Collection<OperationResponse> getJobs(@RequestParam String username,
      @RequestParam String jobName) {

    final var jobInstanceCount = jobExplorer.getJobInstanceCount(jobName);
    final var jobExecution = jobExplorer.getJobInstances(jobName, 0, (int) jobInstanceCount)
        .stream()
        .map(jobExplorer::getLastJobExecution)
        .filter(Objects::nonNull)
        .filter(it -> username.equals(it.getJobParameters().getString("username", "")))
        .sorted(Comparator.comparing(JobExecution::getCreateTime))
        .reduce((a, b) -> b);

    if (jobExecution.isEmpty()) {
      return List.of();
    }

    return jobExplorer.getJobInstances(jobName, 0, (int) jobInstanceCount)
        .stream()
        .map(jobExplorer::getLastJobExecution)
        .filter(Objects::nonNull)
        .map(this::jobExecution2OperationResponse)
        .toList();
  }

  @PostMapping("{id}/stop:remove")
  public void stopAndRemoveJob(@PathVariable Long id) {

    final var jobExecution = jobExplorer.getJobExecution(id);
    if (jobExecution == null) {
      return;
    }

    try {
      if (jobExecution.isRunning()) {
        log.info("停止作业执行: {}", id);
        jobExecution.setStatus(BatchStatus.ABANDONED);
        jobExecution.setExitStatus(ExitStatus.FAILED);
        jobRepository.update(jobExecution);
      }

      final var jobInstance = jobExecution.getJobInstance();

      jobExecution.getStepExecutions().forEach(stepExecution -> {
        log.info("删除 StepExecution {}-{}", stepExecution.getStepName(),
            stepExecution.getId());
        jobRepository.deleteStepExecution(stepExecution);
      });

      jobRepository.deleteJobInstance(jobInstance);
    } catch (Exception e) {
      log.error("停止或移除作业时发生错误: {}", id, e);
    }
  }

  @Operation(summary = "数据源导入")
  @PostMapping("datasource/{id}/user/{username}/uuid/{uuid}:import")
  @SneakyThrows
  public OperationResponse importDatasource(@PathVariable Long id, @PathVariable String username,
      @PathVariable String uuid) {

    final var jobExecution = jobLauncher.run(importDatasourceJob, new JobParametersBuilder()
        .addLong("id", id, false)
        .addString("username", username, false)
        .addString("group_id", uuid, false)
        .addString("instance_id", UUID.randomUUID().toString(), true)
        .toJobParameters());

    return jobExecution2OperationResponse(jobExecution);
  }

  @Operation(summary = "导入实体")
  @PostMapping("vertex:import")
  @SneakyThrows
  public OperationResponse launchImportVertexJob(
      @RequestBody ImportVertexRequest importVertexRequest) {

    final var jobExecution = jobLauncher.run(importVertexJob, new JobParametersBuilder()
        .addString("url", importVertexRequest.getUrl(), false)
        .addString("instance_id", UUID.randomUUID().toString(), true)
        .toJobParameters());

    return jobExecution2OperationResponse(jobExecution);
  }

  private @Nonnull OperationResponse jobExecution2OperationResponse(
      @Nonnull JobExecution jobExecution) {

    final var operationResponse = new OperationResponse();
    operationResponse.setId(jobExecution.getId());
    operationResponse.setStatus(jobExecution.getStatus().toString());
    operationResponse.setExitCode(jobExecution.getExitStatus().getExitCode());
    operationResponse.setName(jobExecution.getJobInstance().getJobName());
    operationResponse.setStartTime(jobExecution.getStartTime() == null ? null
        : Timestamp.valueOf(jobExecution.getStartTime()).getTime());
    operationResponse.setEndTime(jobExecution.getEndTime() == null ? null
        : Timestamp.valueOf(jobExecution.getEndTime()).getTime());
    operationResponse.setParameters(
        jobExecution.getJobParameters().getParameters().entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey,
                // IDEA 的 bug getValue().getValue() 可能为空
                // toMap 的 key 和 value 不可为空
                it -> ObjUtil.isNull(it.getValue().getValue()) ? "" : it.getValue().getValue())));
    jobExecution.getExecutionContext().entrySet()
        .forEach(it -> operationResponse.getCtx().put(it.getKey(), it.getValue()));
    return operationResponse;
  }
}
