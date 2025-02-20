package com.singhand.sr.graphservice.bizkeycloakmodel.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KeycloakClientService {

  private final KeycloakService keycloakService;

  @Autowired
  public KeycloakClientService(KeycloakService keycloakService) {

    this.keycloakService = keycloakService;
  }

  private Long getUserNum() {

    final var total = keycloakService.getRealmResource().users().count();
    return keycloakService.getRealmResource().users().list(0, total)
        .stream().filter(it -> !it.getUsername().startsWith("reserved_")).count();
  }

  public Long getOnlineUserNum() {

    return keycloakService.getRealmResource().getClientSessionStats()
        .stream()
        .filter(it -> it.containsValue(keycloakService.getClientId()))
        .map(it -> Long.parseLong(it.getOrDefault("active", "0")))
        .findFirst()
        .orElse(0L);
  }

  public Long getOfflineUserNum() {

    return getUserNum() - getOnlineUserNum();
  }
}
