package com.singhand.sr.graphservice.bizgraph.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class NewEdgeRequest extends NewEvidenceRequest {

  @Schema(description = "关系名称", requiredMode = RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "关系特征")
  private Map<String, String> features = new HashMap<>();

  @Schema(description = "作用域")
  private String scope = "default";

  private Map<String, String> props = new HashMap<>();
}
