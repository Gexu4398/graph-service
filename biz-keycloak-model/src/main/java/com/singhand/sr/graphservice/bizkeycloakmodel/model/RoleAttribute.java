package com.singhand.sr.graphservice.bizkeycloakmodel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "ROLE_ATTRIBUTE")
public class RoleAttribute {

  @Id
  @Column(name = "ID", nullable = false, length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ROLE_ID", nullable = false)
  @Exclude
  private KeycloakRole role;

  @Column(name = "NAME", nullable = false)
  private String name;

  @Column(name = "VALUE")
  private String value;

}