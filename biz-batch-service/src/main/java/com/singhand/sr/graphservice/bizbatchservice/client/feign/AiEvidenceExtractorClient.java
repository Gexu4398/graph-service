package com.singhand.sr.graphservice.bizbatchservice.client.feign;

import com.singhand.sr.graphservice.bizbatchservice.model.request.AiEvidenceExtractorRequest;
import com.singhand.sr.graphservice.bizbatchservice.model.response.AiEvidenceExtractorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-evidence-extract", url = "app.ai.evidenceUrl")
public interface AiEvidenceExtractorClient {

  @PostMapping("event_extract")
  AiEvidenceExtractorResponse evidenceExtract(@RequestBody AiEvidenceExtractorRequest request);
}
