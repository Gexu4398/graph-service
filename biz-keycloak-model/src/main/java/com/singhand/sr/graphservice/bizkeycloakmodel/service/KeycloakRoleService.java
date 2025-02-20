package com.singhand.sr.graphservice.bizkeycloakmodel.service;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.Role;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.NewRoleRequest;
import java.util.Collection;
import java.util.List;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KeycloakRoleService {

  private final KeycloakService keycloakService;

  @Autowired
  public KeycloakRoleService(KeycloakService keycloakService) {

    this.keycloakService = keycloakService;
  }

  private Role getNewRoleResponse(RoleRepresentation roleRepresentation) {

    return Role.builder()
        .id(roleRepresentation.getId())
        .name(roleRepresentation.getName())
        .scopes(keycloakService.getScopesByRole(roleRepresentation.getName()))
        .build();
  }

  public Role newRole(NewRoleRequest request) {

    final var rr = keycloakService.newRoleResource(request.getName()).toRepresentation();
    keycloakService.attachScopes(request.getScopes(), rr);
    return getNewRoleResponse(rr);
  }

  public Role updateRole(String roleName, Collection<String> scopes) {

    final var rr = keycloakService.getClientRoleResource(roleName).toRepresentation();
    final var currentScopes = keycloakService.getScopesByRole(roleName);
    keycloakService.detachScopes(currentScopes, rr);
    keycloakService.attachScopes(scopes, rr);
    return getNewRoleResponse(rr);
  }

  public Role renameRole(String oldRoleName, String newRoleName) {

    final var newRoleRepresentation = keycloakService.newRoleResource(newRoleName)
        .toRepresentation();
    final var oldRoleRepresentation = keycloakService.getClientRoleResource(oldRoleName)
        .toRepresentation();
    keycloakService.getClientRoleResource(oldRoleName).getUserMembers()
        .forEach(it -> {
          final var username = it.getUsername();
          keycloakService.attachRoleResource(username, List.of(newRoleRepresentation));
          keycloakService.detachRoleResource(username, List.of(oldRoleRepresentation));
        });
    final var scopes = keycloakService.getScopesByRole(oldRoleName);
    keycloakService.getClientResource().roles().deleteRole(oldRoleName);
    keycloakService.attachScopes(scopes, newRoleRepresentation);
    return getNewRoleResponse(newRoleRepresentation);
  }

  public List<Role> getRoles() {

    return keycloakService
        .getClientResource()
        .roles()
        .list()
        .stream()
        .map(it -> Role.builder()
            .id(it.getId())
            .name(it.getName())
            .scopes(keycloakService.getScopesByRole(it.getName()))
            .build())
        .toList();
  }

  public Role getRole(String roleName) {

    final var roleRepresentation = keycloakService
        .getClientRoleResource(roleName)
        .toRepresentation();
    return Role.builder()
        .id(roleRepresentation.getId())
        .name(roleRepresentation.getName())
        .scopes(keycloakService.getScopesByRole(roleName))
        .build();
  }
}