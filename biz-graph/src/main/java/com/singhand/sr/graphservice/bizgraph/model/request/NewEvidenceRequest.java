package com.singhand.sr.graphservice.bizgraph.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.singhand.sr.graphservice.bizgraph.validator.NullOrLongId;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewEvidenceRequest {

  @NotNull
  private String content = "";

  @NullOrLongId
  private Long datasourceId;

  @JsonIgnore
  private String creator;
}
