package com.singhand.sr.graphservice.bizbatchservice.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiTextExtractorResponse {

  private String text;

  private List<Entity> entities = new LinkedList<>();

  private List<AttributeOrRelationship> attributes = new LinkedList<>();

  private List<AttributeOrRelationship> relations = new LinkedList<>();

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Entity {

    @Schema(description = "实体名称")
    private String name;

    @Schema(description = "实体类型")
    private String type;

    @Schema(description = "文本中的起始位置")
    @JsonProperty("start_offset")
    private int startOffset;

    @Schema(description = "文本中的截止位置")
    @JsonProperty("end_offset")
    private int endOffset;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AttributeOrRelationship {

    @Schema(description = "主语实体名称")
    private String subject;

    @Schema(description = "主语实体类型")
    @JsonProperty("subject_type")
    private String subjectType;

    @Schema(description = "关系类型 / 属性类型")
    private String relation;

    @Schema(description = "宾语实体名称 / 属性值")
    private String object;

    @Schema(description = "宾语实体类型")
    @JsonProperty("object_type")
    private String objectType;
  }
}
