package com.singhand.sr.graphservice.bizmodel.model.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
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

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Builder
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "invertex_id", "outvertex_id",
        "scope"}),
    indexes = {
        @Index(columnList = "scope"),
        @Index(columnList = "name"),
        @Index(columnList = "invertex_id"),
        @Index(columnList = "outvertex_id")
    }
)
public class Edge {

  @Id
  @SequenceGenerator(name = "edge_seq", sequenceName = "edge_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "edge_seq")
  @JsonProperty("id")
  private Long ID;

  @Column(nullable = false)
  private String name;

  @Default
  @Column(nullable = false)
  private String scope = "default";

  @ManyToOne(cascade = CascadeType.DETACH)
  private Vertex inVertex;

  @ManyToOne(cascade = CascadeType.DETACH)
  private Vertex outVertex;

  @Default
  @MapKey(name = "key")
  @OneToMany(mappedBy = "edge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Map<String, Property> properties = new HashMap<>();

  @Default
  @OneToMany(mappedBy = "edge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Set<Evidence> evidences = new HashSet<>();

  @Default
  @OneToMany(mappedBy = "edge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @MapKey(name = "key")
  @Exclude
  private Map<String, Feature> features = new HashMap<>();

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @CreationTimestamp
  private Calendar createdAt;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  private Calendar updatedAt;

  @Transient
  @JsonProperty(access = Access.READ_ONLY)
  private Double confidence;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Edge v = (Edge) o;
    return ID != null && Objects.equals(ID, v.ID);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }

  public void detachVertices() {

    getInVertex().getActiveEdges().remove(this);
    getOutVertex().getPassiveEdges().remove(this);
    setInVertex(null);
    setOutVertex(null);
  }

  public void clearProperties() {

    getProperties().forEach((k, v) -> {
      v.setEdge(null);
      v.removeValues(v.getValues());
    });
    getProperties().clear();
  }

  public void addFeature(Feature feature) {

    getFeatures().put(feature.getKey(), feature);
    feature.setEdge(this);
  }

  public boolean isVerified() {

    final var feature = features.get("verified");
    return feature != null && "true".equals(feature.getValue());
  }

  public boolean isChecked() {

    final var feature = features.get("checked");
    return feature != null && "true".equals(feature.getValue());
  }

  public void addProperty(@Nonnull Property property) {

    property.setEdge(this);
    getProperties().put(property.getKey(), property);
  }

  public void addEvidence(Evidence evidence) {

    getEvidences().add(evidence);
    evidence.setEdge(this);
  }

  public void clearEvidences() {

    getEvidences().forEach(it -> {
      it.detachDatasource();
      it.setEdge(null);
      it.detachPicture();
      it.detachPropertyValue();
    });
    getEvidences().clear();
  }
}
