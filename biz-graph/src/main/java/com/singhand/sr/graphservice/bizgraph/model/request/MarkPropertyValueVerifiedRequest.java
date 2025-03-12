package com.singhand.sr.graphservice.bizgraph.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarkPropertyValueVerifiedRequest {

  @NotBlank
  private String key;

  @NotBlank
  private String value;
}
