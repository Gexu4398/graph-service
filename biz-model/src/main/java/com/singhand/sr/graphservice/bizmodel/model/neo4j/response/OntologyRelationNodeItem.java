package com.singhand.sr.graphservice.bizmodel.model.neo4j.response;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OntologyRelationNodeItem {

  private String id;

  private String name;

  private OntologyNode inOntology;

  private OntologyNode outOntology;

  @Schema(description = "创建时间")
  private LocalDateTime createdAt;

  @Schema(description = "更新时间")
  private LocalDateTime updatedAt;
}
