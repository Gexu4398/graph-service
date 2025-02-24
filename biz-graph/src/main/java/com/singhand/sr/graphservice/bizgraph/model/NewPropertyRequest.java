package com.singhand.sr.graphservice.bizgraph.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema
@Getter
@Setter
public class NewPropertyRequest {

  @Schema(description = "属性值")
  @NotBlank(message = "属性值为空")
  private String value;

  @Schema(description = "是否验证")
  private Boolean verified = false;

  @Schema(description = "是否校验")
  private Boolean checked = false;
}
