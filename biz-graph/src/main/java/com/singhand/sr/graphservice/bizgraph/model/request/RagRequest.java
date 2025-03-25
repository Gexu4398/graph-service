package com.singhand.sr.graphservice.bizgraph.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagRequest {

  @NotBlank(message = "问题不能为空")
  private String question;
}
