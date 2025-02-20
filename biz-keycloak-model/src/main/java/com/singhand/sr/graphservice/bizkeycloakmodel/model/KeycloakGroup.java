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
@Table(name = "KEYCLOAK_GROUP")
public class KeycloakGroup {

  @Id
  @Column(name = "ID", nullable = false, length = 36)
  private String id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "PARENT_GROUP", nullable = false, length = 36)
  private String parentGroup;

  @Column(name = "REALM_ID", length = 36)
  private String realmId;

  @ManyToMany(mappedBy = "groups", fetch = FetchType.LAZY)
  @ToString.Exclude
  private Set<UserEntity> users;

}