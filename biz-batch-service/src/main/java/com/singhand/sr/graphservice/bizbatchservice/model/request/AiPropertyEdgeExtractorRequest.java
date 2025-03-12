package com.singhand.sr.graphservice.bizbatchservice.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AiPropertyEdgeExtractorRequest {

  private String text;

  private List<Dict> entities = new LinkedList<>();

  @Data
  public static class Dict {

    @JsonProperty("start_offset")
    private int startOffset = 0;

    @JsonProperty("end_offset")
    private int endOffset = 9;

    @Schema(description = "实体类型")
    private String label;
  }
}
