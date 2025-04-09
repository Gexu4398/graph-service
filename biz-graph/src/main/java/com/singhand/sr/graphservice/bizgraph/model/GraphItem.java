package com.singhand.sr.graphservice.bizgraph.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class GraphItem {

  private List<Entity> entities;

  private List<Relation> relations;

  @Data
  public static class Entity {

    private String name;

    private String type;

    private Map<String, List<String>> properties = new HashMap<>();
  }

  @Data
  public static class Relation {

    private String relationName;

    private String subjectName;

    private String subjectType;

    private String targetName;

    private String targetType;

    private Map<String, List<String>> properties = new HashMap<>();
  }
}
