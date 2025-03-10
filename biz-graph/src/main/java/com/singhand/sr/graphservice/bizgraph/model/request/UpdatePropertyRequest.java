package com.singhand.sr.graphservice.bizgraph.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class UpdatePropertyRequest extends NewEvidenceRequest {

  @Schema(description = "属性名")
  @NotBlank(message = "属性名不能为空")
  private String key;

  @Schema(description = "被修改的属性值")
  @NotBlank(message = "被修改的属性值不能为空")
  private String oldValue;

  @Schema(description = "修改的属性值")
  @NotBlank(message = "修改的属性值不能为空")
  private String newValue;

  @Schema(description = "是否可信")
  private Boolean verified = true;

  @Schema(description = "是否校验")
  private Boolean checked = true;
}
