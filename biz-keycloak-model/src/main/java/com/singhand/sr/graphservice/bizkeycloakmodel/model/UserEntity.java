package com.singhand.sr.graphservice.bizkeycloakmodel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.annotations.Formula;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "USER_ENTITY")
public class UserEntity {

  @Id
  @Column(name = "ID", nullable = false, length = 36)
  private String id;

  @Column(name = "EMAIL")
  private String email;

  @Column(name = "EMAIL_CONSTRAINT")
  private String emailConstraint;

  @Builder.Default
  @Column(name = "EMAIL_VERIFIED", nullable = false)
  private Boolean emailVerified = false;

  @Builder.Default
  @Column(name = "ENABLED", nullable = false)
  private Boolean enabled = false;

  @Column(name = "FEDERATION_LINK")
  private String federationLink;

  @Column(name = "FIRST_NAME")
  private String firstName;

  @Column(name = "LAST_NAME")
  private String lastName;

  @Column(name = "REALM_ID")
  private String realmId;

  @Column(name = "USERNAME")
  private String username;

  @Column(name = "CREATED_TIMESTAMP")
  private Long createdTimestamp;

  @Column(name = "SERVICE_ACCOUNT_CLIENT_LINK")
  private String serviceAccountClientLink;

  @Column(name = "NOT_BEFORE", nullable = false)
  private Integer notBefore;

  @OneToMany(mappedBy = "userEntity", fetch = FetchType.LAZY)
  @Exclude
  private Set<UserAttribute> attributes;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "USER_GROUP_MEMBERSHIP",
      joinColumns = @JoinColumn(name = "USER_ID"),
      inverseJoinColumns = @JoinColumn(name = "GROUP_ID"))
  @Exclude
  private Set<KeycloakGroup> groups;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "USER_ROLE_MAPPING",
      joinColumns = @JoinColumn(name = "USER_ID"),
      inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
  @Exclude
  private Set<KeycloakRole> roles;

  // 超级管理员角色排在首位
  @Formula("case username when 'admin' then 0 else 1 end")
  private int priority;
}