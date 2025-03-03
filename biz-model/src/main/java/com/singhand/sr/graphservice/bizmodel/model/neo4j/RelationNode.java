package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("RelationNode")
@Schema(description = "本体关系模型节点")
public class RelationNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  @Schema(description = "主键")
  private String id;

  @Schema(description = "名称")
  private String name;

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
    RelationNode v = (RelationNode) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
