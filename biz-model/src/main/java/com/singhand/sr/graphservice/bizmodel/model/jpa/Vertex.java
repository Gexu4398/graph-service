package com.singhand.sr.graphservice.bizmodel.model.jpa;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(
    indexes = {
        @Index(name = "idx_vertex_type", columnList = "type"),
        @Index(name = "idx_vertex_name", columnList = "name")
    }
)
@Indexed(index = "vertex_00001")
public class Vertex {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  @JsonProperty("id")
  @KeywordField(name = "id_keyword")
  private String ID;

  @Column(nullable = false)
  @FullTextField(analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  @KeywordField(name = "name_keyword")
  private String name;

  @Column(nullable = false)
  @KeywordField(name = "type_keyword")
  private String type;

  @Default
  @ElementCollection(fetch = FetchType.EAGER)
  @Exclude
  private Set<String> tags = new HashSet<>();

  @Builder.Default
  @OneToMany(mappedBy = "inVertex", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Set<Edge> activeEdges = new HashSet<>();

  @Builder.Default
  @OneToMany(mappedBy = "outVertex", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Set<Edge> passiveEdges = new HashSet<>();

  @Builder.Default
  @MapKey(name = "key")
  @OneToMany(mappedBy = "vertex", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  @IndexedEmbedded(structure = ObjectStructure.NESTED,
      includePaths = {"key_keyword", "values.value_"})
  private Map<String, Property> properties = new HashMap<>();

  @Builder.Default
  @ManyToMany(fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Set<Datasource> datasources = new HashSet<>();

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @CreationTimestamp
  @GenericField(sortable = Sortable.YES)
  private Calendar createdAt;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  @GenericField(sortable = Sortable.YES)
  private Calendar updatedAt;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Vertex vertex = (Vertex) o;
    return ID != null && Objects.equals(ID, vertex.ID);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }

  @JsonIgnore
  public Set<Edge> getEdges() {

    return new HashSet<>(CollUtil.union(getActiveEdges(), getPassiveEdges()));
  }

  public void detachEdges() {

    getActiveEdges().forEach(edge -> {
      edge.getOutVertex().getPassiveEdges().remove(edge);
      edge.setInVertex(null);
      edge.setOutVertex(null);
    });
    getActiveEdges().clear();

    getPassiveEdges().forEach(edge -> {
      edge.getInVertex().getActiveEdges().remove(edge);
      edge.setInVertex(null);
      edge.setOutVertex(null);
    });
    getPassiveEdges().clear();
  }

  public void addProperty(@Nonnull Property property) {

    property.setVertex(this);
    getProperties().put(property.getKey(), property);
  }

  public void detachDataSources() {

    if (null != getDatasources()) {
      getDatasources().forEach(datasource -> datasource.getVertices().remove(this));
      getDatasources().clear();
    }
  }

  public void attachDatasource(Datasource datasource) {

    getDatasources().add(datasource);
    datasource.getVertices().add(this);
  }
}
