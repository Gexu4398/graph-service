package com.singhand.sr.graphservice.bizgraph.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewOntologyPropertyRequest {

  @NotBlank
  private String name;

  @NotBlank
  private String type;
}
