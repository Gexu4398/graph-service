package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import cn.hutool.crypto.digest.MD5;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@EqualsAndHashCode
@Builder
@Node("PropertyValue")
public class PropertyValue {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  private String id;

  private String value;

  @Setter(AccessLevel.NONE)
  private String md5;

  @Relationship(type = "HAS_FEATURE", direction = Relationship.Direction.OUTGOING)
  @Builder.Default
  @JsonIgnore
  private Set<Feature> features = new HashSet<>();

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  public void setValue(String value) {

    this.md5 = MD5.create().digestHex(value);
    this.value = value;
  }
}
