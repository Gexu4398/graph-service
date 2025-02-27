package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.HashSet;
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
@Node("VertexNode")
@Schema(description = "实体节点")
public class VertexNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  @Schema(description = "主键")
  private String id;

  @Schema(description = "名称")
  private String name;

  @Schema(description = "类型")
  private String type;

  @Relationship(type = "HAS_EDGE", direction = Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  @Schema(description = "关系")
  @Exclude
  private Set<EdgeNode> edges = new HashSet<>();

  @Relationship(type = "HAS_PROPERTY", direction = Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  @Schema(description = "属性")
  @Exclude
  private Set<PropertyNode> properties = new HashSet<>();

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
    VertexNode v = (VertexNode) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
