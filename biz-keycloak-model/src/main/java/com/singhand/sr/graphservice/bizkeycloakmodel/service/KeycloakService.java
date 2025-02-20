package com.singhand.sr.graphservice.bizkeycloakmodel.service;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.KeycloakGroupRepository;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.KeycloakRoleRepository;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.UserEntityRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Cleanup;
import lombok.Getter;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(transactionManager = "keycloakTransactionManager")
public class KeycloakService {

  private final Keycloak keycloak;

  @Getter
  private final String realm;

  @Getter
  private final String clientId;

  @Getter
  private final String authServerUrl;

  private final String ID_OF_CLIENT;

  private final UserEntityRepository userEntityRepository;

  private final KeycloakGroupRepository keycloakGroupRepository;

  private final KeycloakRoleRepository keycloakRoleRepository;

  @Autowired
  public KeycloakService(Keycloak keycloak,
      @Value("${keycloak.realm}") String realm,
      @Value("${keycloak.client-id}") String clientId,
      @Value("${keycloak.auth-server-url}") String authServerUrl,
      UserEntityRepository userEntityRepository,
      KeycloakGroupRepository keycloakGroupRepository,
      KeycloakRoleRepository keycloakRoleRepository) {

    this.keycloak = keycloak;
    this.realm = realm;
    this.clientId = clientId;
    this.authServerUrl = authServerUrl;
    this.userEntityRepository = userEntityRepository;
    this.keycloakGroupRepository = keycloakGroupRepository;
    this.ID_OF_CLIENT = getIdOfClient();
    this.keycloakRoleRepository = keycloakRoleRepository;
  }

  public RealmResource getRealmResource() {

    return keycloak.realm(realm);
  }

  public ClientResource getClientResource() {

    final var clientsResource = keycloak.realm(realm).clients();
    final var cr = clientsResource.findByClientId(clientId).stream().findFirst().orElseThrow();
    return clientsResource.get(cr.getId());
  }

  public String getIdOfClient() {

    final var clientsResource = keycloak.realm(realm).clients();
    final var cr = clientsResource.findByClientId(clientId).stream().findFirst().orElseThrow();
    return cr.getId();
  }

  public RoleResource getClientRoleResource(String roleName) {

    final var clientResource = getClientResource();
    final var rolesResource = clientResource.roles();
    return rolesResource.get(roleName);
  }

  public ClientScopesResource getClientScopesResource() {

    return keycloak.realm(realm).clientScopes();
  }

  public UsersResource getUsersResource() {

    return keycloak.realm(realm).users();
  }

  public UserResource getUserResource(String username) {

    final var userEntity = userEntityRepository.findByUsernameAndRealmId(username, getRealm())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在！"));
    final var usersResource = getUsersResource();
    return usersResource.get(userEntity.getId());
  }

  public UserResource getUserResourceById(String id) {

    return getUsersResource().get(id);
  }

  public UserResource newUserResource(String username, String name, String phone, String password,
      boolean enabled) {

    final var userRepresentation = new UserRepresentation();
    userRepresentation.setFirstName(name);
    userRepresentation.setEnabled(enabled);
    userRepresentation.setUsername(username);
    userRepresentation.singleAttribute("phoneNumber", phone);

    final var credential = new CredentialRepresentation();
    credential.setType("password");
    credential.setTemporary(false);
    credential.setValue(password);
    userRepresentation.setCredentials(List.of(credential));

    final var response = getUsersResource().create(userRepresentation);
    final var userId = CreatedResponseUtil.getCreatedId(response);
    return getUsersResource().get(userId);
  }

  public UserResource newUserResource(UserRepresentation userRepresentation) {

    final var response = getUsersResource().create(userRepresentation);
    if (response.getStatus() == 409) {
      final var user = userEntityRepository.findByUsername(userRepresentation.getUsername())
          .orElseThrow();
      getUserResourceById(user.getId()).update(userRepresentation);
      return getUserResource(user.getUsername());
    } else {
      final var userId = CreatedResponseUtil.getCreatedId(response);
      return getUsersResource().get(userId);
    }
  }

  public GroupsResource getGroupsResource() {

    return keycloak.realm(realm).groups();
  }

  public GroupResource renameGroupResource(String id, String name) {

    final var groupRepresentation = getGroupsResource().group(id).toRepresentation();
    groupRepresentation.setName(name);
    getGroupsResource().group(id).update(groupRepresentation);
    return getGroupsResource().group(id);
  }

  public void moveGroupResource(String id, String parentId) {

    final var child = getGroupsResource().group(id).toRepresentation();
    if (null == parentId) {
      getRealmResource().groups().add(child);
    } else {
      getGroupsResource().group(parentId).subGroup(child);
    }
  }

  public GroupResource getGroupResource(String id) {

    return getGroupsResource().group(id);
  }

  public GroupResource newGroupResource(String name, String parentId) {

    final var groupRepresentation = new GroupRepresentation();
    groupRepresentation.setName(name);

    if (StrUtil.isNotBlank(parentId)) {
      final var groupResource = getGroupsResource().group(parentId);
      @Cleanup final var response = groupResource.subGroup(groupRepresentation);
      final var groupId = CreatedResponseUtil.getCreatedId(response);
      return getGroupResource(groupId);
    }

    final var response = getGroupsResource().add(groupRepresentation);
    final var groupId = CreatedResponseUtil.getCreatedId(response);
    return getGroupsResource().group(groupId);
  }

  public RoleResource newRoleResource(String roleName) {

    final var roleRepresentation = new RoleRepresentation();
    roleRepresentation.setName(roleName);
    roleRepresentation.setClientRole(true);
    final var exists = keycloakRoleRepository.existsByNameAndClientRole(roleName, true);
    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "角色名称已存在！");
    }
    getClientResource().roles().create(roleRepresentation);
    return getClientResource().roles().get(roleName);
  }

  public void joinGroup(String userId, String groupId) {

    userEntityRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在！"));
    final var userResource = getUserResourceById(userId);
    if (groupId != null) {
      userResource.groups().forEach(it -> userResource.leaveGroup(it.getId()));
      keycloakGroupRepository.findById(groupId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "部门不存在！"));
      userResource.joinGroup(groupId);
    } else {
      userResource.groups().forEach(it -> userResource.leaveGroup(it.getId()));
    }
  }

  public void attachRoleResource(String username, List<RoleRepresentation> roles) {

    getUserResource(username)
        .roles()
        .clientLevel(ID_OF_CLIENT)
        .add(roles);
  }

  public void detachAllRoleResourceByUserId(String userId) {

    final var userResource = getUserResourceById(userId);
    final var roles = userResource.roles().clientLevel(ID_OF_CLIENT).listAll();
    userResource.roles().clientLevel(ID_OF_CLIENT).remove(roles);
  }

  public void detachAllRoleResource(String username) {

    final var userResource = getUserResource(username);
    final var roles = userResource.roles().clientLevel(ID_OF_CLIENT).listAll();
    userResource.roles().clientLevel(ID_OF_CLIENT).remove(roles);
  }

  public void detachRoleResource(String username, List<RoleRepresentation> roles) {

    getUserResource(username)
        .roles()
        .clientLevel(ID_OF_CLIENT)
        .remove(roles);
  }

  public void addScopeMapping(String idOfScope, RoleRepresentation roleRepresentation) {

    getClientScopesResource()
        .get(idOfScope)
        .getScopeMappings()
        .clientLevel(ID_OF_CLIENT)
        .add(List.of(roleRepresentation));
  }

  public void addRealmScopeMapping(String idOfScope, RoleRepresentation roleRepresentation) {

    getClientScopesResource()
        .get(idOfScope)
        .getScopeMappings()
        .realmLevel()
        .add(List.of(roleRepresentation));
  }

  public void removeScopeMapping(String idOfScope, RoleRepresentation roleRepresentation) {

    getClientScopesResource()
        .get(idOfScope)
        .getScopeMappings()
        .clientLevel(ID_OF_CLIENT)
        .remove(List.of(roleRepresentation));
  }

  public void attachScopes(Collection<String> scopes, RoleRepresentation rr) {

    attachScopes(scopes, rr, false);
  }

  public void attachScopes(Collection<String> scopes, RoleRepresentation rr, boolean isRealmLevel) {

    getClientResource()
        .getDefaultClientScopes()
        .stream()
        .filter(it -> scopes.contains(it.getName()))
        .forEach(it -> {
          if (isRealmLevel) {
            addRealmScopeMapping(it.getId(), rr);
          } else {
            addScopeMapping(it.getId(), rr);
          }
        });
  }

  public void detachScopes(Collection<String> scopes, RoleRepresentation rr) {

    getClientResource()
        .getDefaultClientScopes()
        .stream()
        .filter(it -> scopes.contains(it.getName()))
        .forEach(it -> removeScopeMapping(it.getId(), rr));
  }

  public Set<String> getScopesByRole(String roleName) {

    return getClientResource()
        .getDefaultClientScopes()
        .stream()
        .filter(it -> getClientScopesResource().
            get(it.getId())
            .getScopeMappings()
            .clientLevel(ID_OF_CLIENT)
            .listAll()
            .stream()
            .anyMatch(role -> role.getName().equals(roleName)))
        .map(ClientScopeRepresentation::getName)
        .collect(Collectors.toSet());
  }

  public ClientScopeResource getClientScopeResource(String scopeName) {

    final var clientScopeRepresentation = getClientScopesResource()
        .findAll()
        .stream().filter(it -> it.getName().equals(scopeName))
        .findFirst()
        .orElseThrow();
    return getClientScopesResource().get(clientScopeRepresentation.getId());
  }

  public void attachRoleResourceById(String id, String roleName) {

    final var roleRepresentation = getClientRoleResource(roleName).toRepresentation();
    getUserResourceById(id)
        .roles()
        .clientLevel(ID_OF_CLIENT)
        .add(List.of(roleRepresentation));
  }

  public void acceptUser(String id, String roleName, String memo) {

    final var userRepresentation = getUserResourceById(id).toRepresentation();
    userRepresentation.singleAttribute("status", "normal");
    userRepresentation.singleAttribute("memo", memo);
    getUserResourceById(id).update(userRepresentation);
    attachRoleResourceById(id, roleName);
  }

  public void acceptUser(String id) {

    final var userRepresentation = getUserResourceById(id).toRepresentation();
    userRepresentation.singleAttribute("status", "normal");
    getUserResourceById(id).update(userRepresentation);
  }

  public void rejectUser(String id, String memo) {

    final var userRepresentation = getUserResourceById(id).toRepresentation();
    userRepresentation.singleAttribute("status", "rejected");
    userRepresentation.singleAttribute("memo", memo);
    getUserResourceById(id).update(userRepresentation);
  }

  /**
   * <p>停用用户<p/>
   *
   * @param id 用户id
   */
  public void disableUser(String id) {

    final var userRepresentation = getUserResourceById(id).toRepresentation();
    userRepresentation.setEnabled(false);
    userRepresentation.singleAttribute("status", "disable");
    getUserResourceById(id).update(userRepresentation);
  }

  /**
   * 迁移部门，当用户同时只能在一个部门时调用
   */
  public void migrateGroup(String id, String groupId) {

    joinGroup(id, null);
    if (groupId != null) {
      joinGroup(id, groupId);
    }
  }

  public void deleteSession(String sessionState) {

    keycloak.realm(realm).deleteSession(sessionState, false);
  }
}
