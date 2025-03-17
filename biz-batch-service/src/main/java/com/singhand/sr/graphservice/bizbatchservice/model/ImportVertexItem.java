package com.singhand.sr.graphservice.bizbatchservice.model;

import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
public class ImportVertexItem {

  private List<VertexItem> entities = new LinkedList<>();

  private List<RelationItem> relations = new LinkedList<>();

  @Data
  public static class VertexItem {

    private String name;

    private String type;

    private List<PropertyItem> properties = new LinkedList<>();
  }

  @Data
  public static class PropertyItem {

    private String key;

    private List<String> value = new LinkedList<>();
  }

  @Data
  public static class RelationItem {

    private String name;

    private VertexItem inVertex;

    private VertexItem outVertex;

    private List<PropertyItem> properties = new LinkedList<>();
  }
}
