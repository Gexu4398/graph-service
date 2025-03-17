package com.singhand.sr.graphservice.bizgraph.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOntologyPropertyRequest {

  @NotBlank(message = "属性名称不能为空")
  private String oldName;

  @NotBlank(message = "新属性名称不能为空")
  private String newName;

  @NotBlank
  @NotBlank(message = "属性类型不能为空")
  private String type;

  private boolean multiValue = false;
}
