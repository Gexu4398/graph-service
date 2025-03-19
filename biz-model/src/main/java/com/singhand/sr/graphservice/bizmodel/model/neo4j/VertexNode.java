package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("Vertex")
public class VertexNode {

  @Id
  @NotBlank
  private String id;

  @NotBlank
  @Property
  private String name;

  @NotBlank
  @Property
  private String type;

  @Property
  private String hierarchyLevel;

  @Version
  private Long version;

  @CompositeProperty
  @Default
  @Exclude
  private Map<String, Set<String>> properties = new HashMap<>();

  @Relationship(type = "CONNECTED_TO", direction = Relationship.Direction.OUTGOING)
  @Default
  @Exclude
  private Set<EdgeRelation> edges = new HashSet<>();

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VertexNode v = (VertexNode) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
