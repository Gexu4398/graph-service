package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
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
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Property;
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
  @Property
  private String name;

  @CompositeProperty
  @Default
  @Exclude
  private Map<String, Set<String>> properties = new HashMap<>();

  @TargetNode
  @Exclude
  private VertexNode vertexNode;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EdgeRelation v = (EdgeRelation) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
