package com.singhand.sr.graphservice.bizgraph.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class UpdateEdgeRequest extends NewEvidenceRequest {

  @Schema(description = "原实体id")
  private String oldId;

  @Schema(description = "原关系名称")
  private String oldName;

  @Schema(description = "新关系名称")
  private String newName;

  @Schema(description = "作用域")
  private String scope = "default";

  @Schema(description = "关系特征")
  private Map<String, String> features = new HashMap<>();
}
