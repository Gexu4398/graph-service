package com.singhand.sr.graphservice.bizgraph.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema
@Getter
@Setter
public class NewPropertyRequest extends NewEvidenceRequest {

  @Schema(description = "属性名")
  @JsonIgnore
  private String key;

  @Schema(description = "属性值")
  @NotBlank(message = "属性值不能为空")
  private String value;
}
