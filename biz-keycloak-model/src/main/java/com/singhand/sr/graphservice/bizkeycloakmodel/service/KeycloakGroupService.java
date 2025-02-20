package com.singhand.sr.graphservice.bizkeycloakmodel.service;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.Group;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.KeycloakGroupRepository;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KeycloakGroupService {

  private final KeycloakService keycloakService;

  private final KeycloakGroupRepository keycloakGroupRepository;

  @Autowired
  public KeycloakGroupService(KeycloakService keycloakService,
      KeycloakGroupRepository keycloakGroupRepository) {

    this.keycloakService = keycloakService;
    this.keycloakGroupRepository = keycloakGroupRepository;
  }

  public List<Group> getPosterity(String groupId) {

    if (groupId == null) {
      return keycloakGroupRepository
          .findAll()
          .stream()
          .map(it -> Group.builder()
              .id(it.getId())
              .name(it.getName())
              .build())
          .toList();
    }

    final var result = new LinkedList<Group>();
    final var groups = keycloakGroupRepository
        .findAllByParentGroup(groupId)
        .stream()
        .map(it -> Group.builder()
            .id(it.getId())
            .name(it.getName())
            .build())
        .toList();
    for (final var group : groups) {
      result.addAll(getPosterity(group.getId()));
    }
    result.addAll(groups);
    return result;
  }

  public Group newGroup(Group group) {

    final var groupResource = keycloakService.newGroupResource(group.getName(),
        group.getParentId());
    final var groupRepresentation = groupResource.toRepresentation();
    return getGroup(groupRepresentation.getId());
  }

  public Group renameGroup(String id, String newName) {

    keycloakService.renameGroupResource(id, newName);
    return getGroup(id);
  }

  public Group moveGroup(String id, String parentId) {

    keycloakService.moveGroupResource(id, parentId);
    return getGroup(id);
  }

  public Group getGroup(String id) {

    final var groupResource = keycloakService.getGroupResource(id);
    final var groupRepresentation = groupResource.toRepresentation();
    return Group.builder()
        .id(groupRepresentation.getId())
        .name(groupRepresentation.getName())
        .build();
  }

  public List<Group> getGroups() {

    return keycloakGroupRepository
        .findAll()
        .stream()
        .map(it -> Group.builder()
            .id(it.getId())
            .name(it.getName())
            .parentId(it.getParentGroup())
            .build())
        .toList();
  }

  public void deleteGroup(String id) {

    final var groupResource = keycloakService.getGroupResource(id);
    groupResource.remove();
  }
}
