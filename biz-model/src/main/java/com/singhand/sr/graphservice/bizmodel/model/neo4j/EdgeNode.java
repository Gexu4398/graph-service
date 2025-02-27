package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("EdgeNode")
@Schema(description = "实体关系节点")
public class EdgeNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  @Schema(description = "主键")
  private String id;

  @Schema(description = "关系名称")
  private String name;

  @Relationship(type = "RELATES_TO", direction = Relationship.Direction.OUTGOING)
  @NotNull
  @Schema(description = "对象节点")
  private VertexNode targetVertexNode;

  @Relationship(type = "HAS_PROPERTY", direction = Relationship.Direction.OUTGOING)
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
    EdgeNode v = (EdgeNode) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
