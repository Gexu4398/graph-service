package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RelationshipProperties
public class EdgeRelation {

  @RelationshipId
  private Long id;

  @NotBlank
  private String name;

  @TargetNode
  @Exclude
  private VertexNode vertexNode;
}
