package com.singhand.sr.graphservice.bizmodel.model.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.Builder.Default;
import lombok.ToString.Exclude;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Builder
@Indexed(index = "datasource_00001")
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
  @FullTextField(highlightable = Highlightable.ANY, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  @KeywordField(name = "title_keyword")
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column
  private String source;

  @Column(length = 2000)
  private String url;

  @Column(nullable = false)
  private String status = STATUS_PENDING;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @Exclude
  @JsonIgnore
  @IndexedEmbedded(structure = ObjectStructure.NESTED, includePaths = {"text"})
  private DatasourceContent datasourceContent;

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
  @GenericField(sortable = Sortable.YES)
  private Calendar createdAt;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @UpdateTimestamp
  @JsonProperty(access = Access.READ_ONLY)
  @GenericField(sortable = Sortable.YES)
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
    Datasource v = (Datasource) o;
    return ID != null && Objects.equals(ID, v.ID);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }

  public void attachContent(@Nonnull DatasourceContent datasourceContent) {

    datasourceContent.setDatasource(this);
    setDatasourceContent(datasourceContent);
  }

  public void detachContent() {

    if (null != getDatasourceContent()) {
      getDatasourceContent().setDatasource(null);
      setDatasourceContent(null);
    }
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
