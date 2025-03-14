package com.singhand.sr.graphservice.bizgraph.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class NewVertexRequest extends NewEvidenceRequest {

  @Schema(description = "实体名称")
  @NotBlank(message = "实体名称为空")
  private String name;

  @Schema(description = "实体类型")
  @NotBlank(message = "实体类型为空")
  private String type;

  @Default
  private Map<String, String> props = new HashMap<>();
}
