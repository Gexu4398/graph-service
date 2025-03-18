package com.singhand.sr.graphservice.bizbatchservice.model;

import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportVertexItem {

  @Default
  private List<VertexItem> entities = new LinkedList<>();

  @Default
  private List<RelationItem> relations = new LinkedList<>();

  @Setter
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class VertexItem {

    private String name;

    private String type;

    @Default
    private List<PropertyItem> properties = new LinkedList<>();
  }

  @Setter
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PropertyItem {

    private String key;

    @Default
    private List<String> value = new LinkedList<>();
  }

  @Setter
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RelationItem {

    private String name;

    private VertexItem inVertex;

    private VertexItem outVertex;

    @Default
    private List<PropertyItem> properties = new LinkedList<>();
  }
}
