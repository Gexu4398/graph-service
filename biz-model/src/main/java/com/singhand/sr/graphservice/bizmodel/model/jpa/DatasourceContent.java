package com.singhand.sr.graphservice.bizmodel.model.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Builder
public class DatasourceContent {

  @Id
  @SequenceGenerator(name = "datasourcecontent_seq", sequenceName = "datasourcecontent_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "datasourcecontent_seq")
  @JsonProperty("id")
  private Long ID;

  @Lob
  @Column(columnDefinition = "text")
  private String text;

  @Lob
  @Column(columnDefinition = "text")
  private String html;

  @OneToOne(mappedBy = "datasourceContent")
  @JsonIgnore
  private Datasource datasource;
}
