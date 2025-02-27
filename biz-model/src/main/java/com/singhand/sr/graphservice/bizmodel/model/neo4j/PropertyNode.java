package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@Node("PropertyNode")
public class PropertyNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  private String id;

  private String name;

  @Relationship(type = "HAS_VALUE", direction = Relationship.Direction.OUTGOING)
  @Builder.Default
  private Set<PropertyValueNode> values = new HashSet<>();
}
