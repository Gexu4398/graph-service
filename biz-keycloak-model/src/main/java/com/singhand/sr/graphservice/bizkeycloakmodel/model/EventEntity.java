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
@Table(name = "EVENT_ENTITY")
public class EventEntity {

  @Id
  @Column(name = "ID", nullable = false, length = 36)
  private String id;

  @Column(name = "CLIENT_ID")
  private String clientId;

  @Column(name = "DETAILS_JSON", length = 2550)
  private String detailsJson;

  @Column(name = "ERROR")
  private String error;

  @Column(name = "IP_ADDRESS")
  private String ipAddress;

  @Column(name = "REALM_ID")
  private String realmId;

  @Column(name = "SESSION_ID")
  private String sessionId;

  @Column(name = "EVENT_TIME")
  private Long eventTime;

  @Column(name = "TYPE")
  private String type;

  @Column(name = "USER_ID")
  private String userId;
}
