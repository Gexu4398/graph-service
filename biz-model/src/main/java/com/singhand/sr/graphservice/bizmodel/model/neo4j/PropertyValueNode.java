package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import cn.hutool.crypto.digest.MD5;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
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
@Node("PropertyValueNode")
@Schema(description = "属性值节点")
public class PropertyValueNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  @Schema(description = "主键")
  private String id;

  @Schema(description = "属性值")
  private String value;

  @Setter(AccessLevel.NONE)
  @Schema(description = "属性值MD5，用于校验属性值重复，防止长属性值的匹配")
  private String md5;

  @Relationship(type = "HAS_FEATURE", direction = Relationship.Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  @Schema(description = "特征")
  @Exclude
  private Set<FeatureNode> features = new HashSet<>();

  @CreatedDate
  @Schema(description = "创建时间")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Schema(description = "更新时间")
  private LocalDateTime updatedAt;

  public void setValue(String value) {

    this.md5 = MD5.create().digestHex(value);
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyValueNode v = (PropertyValueNode) o;
    return id != null && Objects.equals(id, v.id);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
