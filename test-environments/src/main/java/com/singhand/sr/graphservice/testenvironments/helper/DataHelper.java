package com.singhand.sr.graphservice.testenvironments.helper;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.Group;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.Role;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.User;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.UserEntity;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.NewRoleRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.NewUserRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.UserEntityRepository;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakGroupService;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakRoleService;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakUserService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Validated
@Slf4j
public class DataHelper {

  public final static String NAME_PREFIX = "";

  private final KeycloakUserService keycloakUserService;

  private final KeycloakGroupService keycloakGroupService;

  private final KeycloakRoleService keycloakRoleService;

  private final UserEntityRepository userEntityRepository;

  private final OntologyService ontologyService;

  private final OntologyRepository ontologyRepository;

  @Autowired
  public DataHelper(KeycloakUserService keycloakUserService,
      KeycloakGroupService keycloakGroupService, KeycloakRoleService keycloakRoleService,
      UserEntityRepository userEntityRepository, OntologyService ontologyService,
      OntologyRepository ontologyRepository) {

    this.keycloakUserService = keycloakUserService;
    this.keycloakGroupService = keycloakGroupService;
    this.keycloakRoleService = keycloakRoleService;
    this.userEntityRepository = userEntityRepository;
    this.ontologyService = ontologyService;
    this.ontologyRepository = ontologyRepository;
  }

  public Optional<UserEntity> getUser(String username) {

    return userEntityRepository.findByUsername(username);
  }

  public User newUser(String username, String password) {

    final var request = new NewUserRequest();
    request.setUsername(username);
    request.setPassword(password);
    return keycloakUserService.newUser(request);
  }

  public User newUser(String username, String password, String groupId, String roleId) {

    final var request = new NewUserRequest();
    request.setUsername(username);
    request.setPassword(password);
    request.setGroupId(groupId);
    if (StrUtil.isNotBlank(roleId)) {
      request.setRoleId(Set.of(roleId));
    }
    return keycloakUserService.newUser(request);
  }

  public String newGroup(String name, String parentId) {

    final var group = new Group();
    group.setName(name);
    group.setParentId(parentId);
    return keycloakGroupService.newGroup(group).getId();
  }

  public Role newRole(String name) {

    final var request = new NewRoleRequest();
    request.setName(name);
    request.setScopes(List.of("user:crud"));
    return keycloakRoleService.newRole(request);
  }

  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Ontology newOntology(String name, String parent) {

    if (StrUtil.isNotBlank(parent)) {
      final var parentOntology = ontologyRepository.findByName(parent)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      return ontologyService.newOntology(name, parentOntology.getID());
    }
    return ontologyService.newOntology(name, null);
  }

  public void newOntologyProperty(Ontology ontology, String key) {

    ontologyService.newOntologyProperty(ontology, key);
  }
}
