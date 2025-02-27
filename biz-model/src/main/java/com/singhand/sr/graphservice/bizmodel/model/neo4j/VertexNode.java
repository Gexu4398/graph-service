package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@Node("VertexNode")
public class VertexNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  private String id;

  private String name;

  private String type;

  @Relationship(type = "HAS_EDGE", direction = Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  private Set<EdgeNode> edges = new HashSet<>();

  @Relationship(type = "HAS_PROPERTY", direction = Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  private Set<PropertyNode> properties = new HashSet<>();

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;
}
