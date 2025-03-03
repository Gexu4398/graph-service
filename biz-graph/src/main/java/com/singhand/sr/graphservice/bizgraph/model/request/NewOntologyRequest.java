package com.singhand.sr.graphservice.bizgraph.model.request;

import lombok.Data;

@Data
public class NewOntologyRequest {

  private String name;

  private String parentId;
}
