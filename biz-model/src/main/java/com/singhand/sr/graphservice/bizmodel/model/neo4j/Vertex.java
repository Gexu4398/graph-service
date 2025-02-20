package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import java.time.Instant;
import lombok.AllArgsConstructor;
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
@Node
public class Vertex {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  private String id;

  private String name;

  private String type;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updateAt;
}
