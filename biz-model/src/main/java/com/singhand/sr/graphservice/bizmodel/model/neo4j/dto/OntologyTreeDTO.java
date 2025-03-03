package com.singhand.sr.graphservice.bizmodel.model.neo4j.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "本体树节点")
public class OntologyTreeDTO {

  @Schema(description = "节点ID")
  private String id;

  @Schema(description = "节点名称")
  private String name;

  @Schema(description = "子节点列表")
  private List<OntologyTreeDTO> childOntologies = new LinkedList<>();

  @Schema(description = "创建时间")
  private LocalDateTime createdAt;

  @Schema(description = "更新时间")
  private LocalDateTime updatedAt;
}
