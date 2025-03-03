package com.singhand.sr.graphservice.bizgraph.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewOntologyPropertyRequest {

  @NotBlank(message = "名称不能为空")
  private String name;

  @NotBlank(message = "类型不能为空")
  private String type;

  private boolean multiple = false;

  private String description;
}
