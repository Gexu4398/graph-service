package com.singhand.sr.graphservice.bizmodel.model.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Comparator;
import java.util.HashSet;
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
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Builder
@Table(indexes = {
    @Index(columnList = "vertex_ID, key_", unique = true),
    @Index(columnList = "edge_ID, key_", unique = true)
})
public class Property {

  @Id
  @SequenceGenerator(name = "property_seq", sequenceName = "property_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "property_seq")
  @JsonProperty("id")
  private Long ID;

  @Column(name = "key_", nullable = false)
  @KeywordField(name = "key_keyword")
  private String key;

  @Builder.Default
  @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @Exclude
  @IndexedEmbedded(structure = ObjectStructure.NESTED, includePaths = {"value_"})
  private Set<PropertyValue> values = new HashSet<>();

  @ManyToOne
  @JsonIgnore
  private Edge edge;

  @ManyToOne
  @JsonIgnore
  private Vertex vertex;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Property v = (Property) o;
    return ID != null && Objects.equals(ID, v.ID);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }

  @JsonIgnore
  public PropertyValue getMaxConfidenceOrBlankValue() {

    return values.stream()
        .max(Comparator.comparing(PropertyValue::getConfidence))
        .orElse(new PropertyValue());
  }

  public void addValue(@Nonnull PropertyValue propertyValue) {

    propertyValue.setProperty(this);
    getValues().add(propertyValue);
  }

  public void removeValue(@Nonnull PropertyValue propertyValue) {

    propertyValue.setProperty(null);
    getValues().remove(propertyValue);
  }

  public void removeValues(@Nonnull Set<PropertyValue> values) {

    values.forEach(value -> value.setProperty(null));
    values.clear();
  }

  public void clearValues() {

    if (null != getValues()) {
      getValues().forEach(value -> value.setProperty(null));
      getValues().clear();
    }
  }
}
