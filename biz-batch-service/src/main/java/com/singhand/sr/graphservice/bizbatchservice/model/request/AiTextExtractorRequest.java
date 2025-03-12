package com.singhand.sr.graphservice.bizbatchservice.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
public class AiTextExtractorRequest {

  @JsonProperty("text_list")
  private List<String> texts = new LinkedList<>();
}
