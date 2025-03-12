package com.singhand.sr.graphservice.bizbatchservice.model.request;

import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
public class AiEvidenceExtractorRequest {

  private List<String> texts = new LinkedList<>();
}
