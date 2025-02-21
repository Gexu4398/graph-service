package com.singhand.sr.graphservice.bizgraph.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class NewVertexRequest {

  @NotBlank(message = "类型为空")
  private String type;

  @NotBlank(message = "名称为空")
  private String name;
}
