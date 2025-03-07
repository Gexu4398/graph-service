package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("Ontology")
public class OntologyNode {

  @Id
  private Long id;

  @Property
  private String name;

  @Version
  private Long version;

  @Relationship(type = "CHILD_OF", direction = Direction.OUTGOING)
  private Set<OntologyNode> children = new HashSet<>();
}
