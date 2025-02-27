package com.singhand.sr.graphservice.bizmodel.model.neo4j;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@RelationshipProperties
@Schema(description = "本体属性节点")
public class OntologyPropertyNode {

  @Id
  @GeneratedValue(UUIDStringGenerator.class)
  @Schema(description = "主键")
  private String id;

  @Schema(description = "名称")
  @NotBlank
  private String name;

  @Schema(description = "类型")
  @NotBlank
  private String type;

  @Schema(description = "是否多值")
  @Builder.Default
  private boolean multiple = false;

  @Schema(description = "描述")
  private String description;
}
