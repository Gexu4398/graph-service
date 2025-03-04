package com.singhand.sr.graphservice.bizmodel.model.jpa;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
public class Vertex {

  @Id
  @Column(nullable = false)
  @JsonProperty("id")
  private String ID;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String type;

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
  private Map<String, Property> properties = new HashMap<>();

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @CreationTimestamp
  private Calendar createdAt;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
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
}
