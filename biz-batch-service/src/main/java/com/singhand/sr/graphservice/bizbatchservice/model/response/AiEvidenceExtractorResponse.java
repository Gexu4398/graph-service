package com.singhand.sr.graphservice.bizbatchservice.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiEvidenceExtractorResponse {

  private List<Map<String, List<Label>>> labels = new LinkedList<>();

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Label {

    private int start;

    private int end;

    private String text;

    private Map<String, List<Label>> relations = new HashMap<>();

    @JsonProperty("事件类型")
    private String eventType;
  }
}
