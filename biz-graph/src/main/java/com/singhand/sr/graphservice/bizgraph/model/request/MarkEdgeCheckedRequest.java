package com.singhand.sr.graphservice.bizgraph.model.request;

import lombok.Data;

@Data
public class MarkEdgeCheckedRequest {

  private String name;

  private String scope = "default";
}
