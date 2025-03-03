package com.singhand.sr.graphservice.bizmodel.model.neo4j.response;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OntologyPropertyItem {

  private String id;

  private String name;

  private String type;

  private boolean multiple = false;

  private String description;

  @Schema(description = "是否为父类属性")
  private boolean parent = false;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
