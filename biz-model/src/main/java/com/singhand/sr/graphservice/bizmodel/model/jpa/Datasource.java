package com.singhand.sr.graphservice.bizmodel.model.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.singhand.sr.graphservice.bizmodel.validator.In;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import java.util.Calendar;
import java.util.HashSet;
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
@Table(indexes = {
    @Index(name = "idx_datasource_createdAt", columnList = "createdAt")
})
@Schema
public class Datasource {

  // 待抽取
  public final static String STATUS_PENDING = "pending";

  // 抽取中
  public final static String STATUS_PROCESSING = "processing";

  // 抽取成功
  public final static String STATUS_SUCCESS = "success";

  // 抽取失败
  public final static String STATUS_FAILURE = "failure";

  @Id
  @SequenceGenerator(name = "datasource_seq", sequenceName = "datasource_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "datasource_seq")
  @JsonProperty("id")
  private Long ID;

  @Column(nullable = false)
  @NotBlank(message = "标题为空")
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column
  private String source;

  @Column(length = 2000)
  private String url;

  @Column(nullable = false)
  @In(set = {"pending", "processing", "success", "failure"})
  @Default
  private String status = STATUS_PENDING;

  @Column(nullable = false)
  @Default
  private Double confidence = 0.0;

  @Column(nullable = false)
  private String creator;

  @Default
  @ManyToMany(mappedBy = "datasources", fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Set<Vertex> vertices = new HashSet<>();

  @Default
  @OneToMany(mappedBy = "datasource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Exclude
  private Set<Evidence> evidences = new HashSet<>();

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @CreationTimestamp
  @JsonProperty(access = Access.READ_ONLY)
  private Calendar createdAt;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  @JsonProperty(access = Access.READ_ONLY)
  private Calendar updatedAt;

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Datasource v = (Datasource) o;
    return ID != null && Objects.equals(ID, v.ID);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }

  public void addEvidence(@Nonnull Evidence evidence) {

    evidence.setDatasource(this);
    getEvidences().add(evidence);
  }

  public void detachVertex(@Nonnull Vertex vertex) {

    vertex.getDatasources().remove(this);
    getVertices().remove(vertex);
  }

  public void clearEvidences() {

    getEvidences().forEach(evidence -> {
      evidence.detachPicture();
      evidence.detachPropertyValue();
      evidence.detachEdge();
      evidence.setDatasource(null);
    });
    getEvidences().clear();
  }
}
