package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
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
public class OntologyRelation {

  @RelationshipId
  private String id;

  @NotBlank
  @Property
  private String name;

  @TargetNode
  @Exclude
  private OntologyNode ontologyNode;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OntologyRelation v = (OntologyRelation) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
