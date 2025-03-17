package com.singhand.sr.graphservice.bizgraph.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportVertexRequest {

  @NotBlank(message = "文件路径不能为空")
  private String url;
}
