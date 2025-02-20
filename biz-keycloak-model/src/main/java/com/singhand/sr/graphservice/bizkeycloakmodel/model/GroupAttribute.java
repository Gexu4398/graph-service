package com.singhand.sr.graphservice.bizkeycloakmodel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "GROUP_ATTRIBUTE")
public class GroupAttribute {

  @Id
  @Column(name = "ID", nullable = false, length = 36)
  private String id;

  @Column(name = "NAME", nullable = false)
  private String name;

  @Column(name = "VALUE")
  private String value;

}