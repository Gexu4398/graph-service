package com.singhand.sr.graphservice.bizkeycloakmodel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;
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
@Table(name = "KEYCLOAK_ROLE")
public class KeycloakRole {

  @Id
  @Column(name = "ID", nullable = false, length = 36)
  private String id;

  @Column(name = "CLIENT_REALM_CONSTRAINT")
  private String clientRealmConstraint;

  @Column(name = "CLIENT_ROLE")
  private Boolean clientRole;

  @Column(name = "DESCRIPTION")
  private String description;

  @Column(name = "NAME")
  private String name;

  @Column(name = "REALM_ID")
  private String realmId;

  @Column(name = "CLIENT", length = 36)
  private String client;

  @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
  @ToString.Exclude
  private Set<UserEntity> users;

}