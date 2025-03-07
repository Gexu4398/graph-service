package com.singhand.sr.graphservice.bizmodel.model.jpa;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Calendar;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Builder
@Table(indexes = @Index(columnList = "url"))
public class Picture {

  @Id
  @SequenceGenerator(name = "picture_seq", sequenceName = "picture_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "picture_seq")
  @JsonProperty("id")
  private Long ID;

  @Column(nullable = false, columnDefinition = "text")
  private String url;

  @Column(columnDefinition = "text")
  private String information;

  @Column(nullable = false)
  @JsonProperty(access = Access.READ_ONLY)
  private String creator;

  @Builder.Default
  @OneToMany(mappedBy = "picture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonProperty(access = Access.WRITE_ONLY)
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
    Picture v = (Picture) o;
    return ID != null && Objects.equals(ID, v.ID);
  }

  @Override
  public int hashCode() {

    return getClass().hashCode();
  }
}
