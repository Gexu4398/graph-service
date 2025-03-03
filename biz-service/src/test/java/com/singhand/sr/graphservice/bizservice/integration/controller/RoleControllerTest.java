package com.singhand.sr.graphservice.bizservice.integration.controller;

import cn.hutool.json.JSONUtil;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.NewRoleRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.RenameRoleRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.KeycloakRoleRepository;
import com.singhand.sr.graphservice.testenvironments.KeycloakIntegrationTestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WithMockUser(username = "admin", authorities = "role:crud")
class RoleControllerTest extends KeycloakIntegrationTestEnvironment {

  @Autowired
  private KeycloakRoleRepository keycloakRoleRepository;

  @Test
  @SneakyThrows
  void testNewRole() {

    final var request = new NewRoleRequest();
    request.setName(faker.name().title());
    request.setScopes(List.of("role:crud"));

    mockMvc.perform(post("/role")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(request.getName())));
  }

  @Test
  @SneakyThrows
  void testUpdateRole() {

    final var role = dataHelper.newRole(faker.name().title());
    mockMvc.perform(put("/role/" + role.getName())
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(Set.of("role:crud", "department:crud"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scopes.length()", equalTo(2)))
        .andExpect(jsonPath("$.scopes", hasItem("role:crud")))
        .andExpect(jsonPath("$.scopes", hasItem("department:crud")));
  }

  @Test
  @SneakyThrows
  void testGetRoles() {

    dataHelper.newRole(faker.name().title());
    dataHelper.newRole(faker.name().title());
    dataHelper.newRole(faker.name().title());

    mockMvc.perform(get("/role"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", equalTo(4)))
        .andExpect(jsonPath("$[0].name", equalTo("超级管理员")));
  }

  @Test
  @SneakyThrows
  void testGetRole() {

    final var role = dataHelper.newRole(faker.name().title());

    mockMvc.perform(get("/role/" + role.getName()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(role.getId())))
        .andExpect(jsonPath("$.name", equalTo(role.getName())))
        .andExpect(jsonPath("$.scopes.length()", equalTo(role.getScopes().size())));
  }

  @Test
  @SneakyThrows
  void testDeleteRole() {

    final var role = dataHelper.newRole(faker.name().title());

    mockMvc.perform(delete("/role/" + role.getName()))
        .andExpect(status().isOk());

    mockMvc.perform(head("/role")
            .param("roleName", role.getName()))
        .andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void testRenameRole() {

    final var role = dataHelper.newRole(faker.name().title());

    final var request = new RenameRoleRequest();
    request.setNewRoleName(faker.name().title());

    mockMvc.perform(post("/role/" + role.getName() + ":rename")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(request.getNewRoleName())));
  }
}
