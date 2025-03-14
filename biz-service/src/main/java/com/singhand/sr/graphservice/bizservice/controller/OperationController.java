package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizmodel.model.reponse.OperationResponse;
import com.singhand.sr.graphservice.bizservice.client.feign.BizBatchServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "异步任务执行状态查询")
@RestController
@RequestMapping("operation")
public class OperationController {

  private final BizBatchServiceClient bizBatchServiceClient;

  @Autowired
  public OperationController(BizBatchServiceClient bizBatchServiceClient) {

    this.bizBatchServiceClient = bizBatchServiceClient;
  }

  @GetMapping("{id}")
  @PreAuthorize("isAuthenticated()")
  public OperationResponse getJobExecution(@PathVariable Long id) {

    return bizBatchServiceClient.getExecutionStatus(id);
  }

  @GetMapping("{name}/uuid/{uuid}")
  @PreAuthorize("isAuthenticated()")
  public OperationResponse getJobExecutionByJobNameAndUuid(@PathVariable String name,
      @PathVariable String uuid) {

    return bizBatchServiceClient.getExecutionStatusByJobNameAndUuid(name, uuid);
  }

  @Operation(summary = "停止并移除任务")
  @PostMapping("{id}/stop:remove")
  @PreAuthorize("isAuthenticated()")
  public void stopAndRemoveJob(@PathVariable Long id) {

    bizBatchServiceClient.stopAndRemoveJob(id);
  }
}
