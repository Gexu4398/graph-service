package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("PropertyNode")
@Schema(description = "属性节点")
public class PropertyNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  @Schema(description = "主键")
  private String id;

  @Schema(description = "属性名称")
  private String name;

  @Relationship(type = "HAS_VALUE", direction = Relationship.Direction.OUTGOING)
  @Builder.Default
  @Schema(description = "属性值")
  @Exclude
  private Set<PropertyValueNode> values = new HashSet<>();

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyNode v = (PropertyNode) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
