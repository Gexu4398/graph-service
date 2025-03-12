package com.singhand.sr.graphservice.bizbatchservice.client.feign;

import com.singhand.sr.graphservice.bizbatchservice.model.request.AiPropertyEdgeExtractorRequest;
import com.singhand.sr.graphservice.bizbatchservice.model.request.AiTextExtractorRequest;
import com.singhand.sr.graphservice.bizbatchservice.model.response.AiTextExtractorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-text-extract", url = "${app.ai.textUrl}")
public interface AiTextExtractorClient {

  @PostMapping("extract")
  AiTextExtractorResponse propertyEdgeExtract(@RequestBody AiPropertyEdgeExtractorRequest request);

  @PostMapping("extract_knowledge")
  AiTextExtractorResponse informationExtract(@RequestBody AiTextExtractorRequest request);
}
