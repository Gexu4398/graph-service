package com.singhand.sr.graphservice.bizservice.client.feign;

import com.singhand.sr.graphservice.bizmodel.model.reponse.OperationResponse;
import java.util.Collection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "biz-batch-service", path = "/api/v1")
public interface BizBatchServiceClient {

  @GetMapping("/batch-job/{id}")
  OperationResponse getExecutionStatus(@PathVariable Long id);

  @GetMapping("/batch-job/{name}/uuid/{uuid}")
  OperationResponse getExecutionStatusByJobNameAndUuid(@PathVariable String name,
      @PathVariable String uuid);

  @GetMapping("/batch-job/latest")
  Collection<OperationResponse> getJobs(@RequestParam String username,
      @RequestParam String jobName);
}
