package com.singhand.sr.graphservice.bizmodel.model.jpa;

import cn.hutool.crypto.digest.MD5;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Builder
public class PropertyValue {

  @Id
  @SequenceGenerator(name = "propertyvalue_seq", sequenceName = "propertyvalue_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "propertyvalue_seq")
  @JsonProperty("id")
  private Long ID;

  @Column(name = "value_", columnDefinition = "text")
  @FullTextField(name = "value_", analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String value;

  @Column(nullable = false)
  @Setter(AccessLevel.NONE)
  private String md5;

  @ManyToOne
  @JsonIgnore
  private Property property;

  @Default
  @OneToMany(mappedBy = "propertyValue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Set<Evidence> evidences = new HashSet<>();

  @Default
  @OneToMany(mappedBy = "propertyValue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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
    PropertyValue v = (PropertyValue) o;
    return ID != null && Objects.equals(ID, v.ID);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }

  public boolean isVerified() {

    final var feature = features.get("verified");
    return feature != null && "true".equals(feature.getValue());
  }

  @JsonProperty(value = "checked", access = Access.READ_ONLY)
  public boolean isChecked() {

    final var feature = features.get("checked");
    return feature != null && "true".equals(feature.getValue());
  }

  public void setValue(String value) {

    assert property != null;
    md5 = MD5.create().digestHex(value);
    this.value = value;
  }

  public void addFeature(Feature feature) {

    getFeatures().put(feature.getKey(), feature);
    feature.setPropertyValue(this);
  }

  public void addEvidence(Evidence evidence) {

    getEvidences().add(evidence);
    evidence.setPropertyValue(this);
  }

  public void clearEvidences() {

    getEvidences().forEach(it -> {
      it.detachEdge();
      it.setPropertyValue(null);
      it.detachPicture();
      it.detachDatasource();
    });
    getEvidences().clear();
  }
}
