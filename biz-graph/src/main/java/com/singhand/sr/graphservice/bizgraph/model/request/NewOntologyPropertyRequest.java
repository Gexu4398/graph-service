package com.singhand.sr.graphservice.bizgraph.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewOntologyPropertyRequest {

  @NotBlank(message = "属性名称不能为空")
  private String name;

  @NotBlank
  @NotBlank(message = "属性类型不能为空")
  private String type;

  private boolean multiValue = false;

  @JsonIgnore
  private boolean inherited = false;
}
