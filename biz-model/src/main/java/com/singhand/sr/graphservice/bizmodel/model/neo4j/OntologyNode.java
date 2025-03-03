package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;
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
@Builder
@Node("OntologyNode")
@Schema(description = "本体节点")
public class OntologyNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  @Schema(description = "主键")
  private String id;

  @Schema(description = "关系名称")
  private String name;

  @Relationship(type = "HAS_PROPERTY", direction = Direction.OUTGOING)
  @Builder.Default
  @Schema(description = "属性")
  @Exclude
  private Set<OntologyPropertyNode> properties = new HashSet<>();

  @Relationship(type = "HAS_RELATION", direction = Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  @Schema(description = "关系")
  @Exclude
  private Set<RelationNode> relationNodes = new HashSet<>();

  @Relationship(type = "HAS_CHILD", direction = Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  @Schema(description = "子节点")
  @Exclude
  private Set<OntologyNode> childOntologies = new HashSet<>();

  @Relationship(type = "HAS_CHILD", direction = Direction.INCOMING)
  @JsonIgnore
  @Schema(description = "父节点")
  private OntologyNode parent;

  @CreatedDate
  @Schema(description = "创建时间")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Schema(description = "更新时间")
  private LocalDateTime updatedAt;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OntologyNode v = (OntologyNode) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
