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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@Node("Edge")
public class Edge {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "RELATES_TO", direction = Relationship.Direction.OUTGOING)
  private Vertex targetVertex;

  @Relationship(type = "HAS_PROPERTY", direction = Relationship.Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  private Set<Property> properties = new HashSet<>();

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;
}
