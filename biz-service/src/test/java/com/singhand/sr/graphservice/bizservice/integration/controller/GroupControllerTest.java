package com.singhand.sr.graphservice.bizservice.integration.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.Group;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.RenameGroupRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakGroupService;
import com.singhand.sr.graphservice.testenvironments.KeycloakIntegrationTestEnvironment;
import com.singhand.sr.graphservice.testenvironments.helper.DataHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

@Slf4j
@WithMockUser(username = "admin", authorities = "department:crud")
class GroupControllerTest extends KeycloakIntegrationTestEnvironment {

  @Autowired
  private KeycloakGroupService keycloakGroupService;

  @Autowired
  private DataHelper dataHelper;

  @Test
  @SneakyThrows
  void testNewGroup() {

    final var group = new Group();
    group.setName(faker.team().name());

    mockMvc.perform(post("/department")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(group)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/department"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", equalTo(1)))
        .andExpect(jsonPath("$.[?(@.name=='" + group.getName() + "')]", hasSize(1)));
  }

  @Test
  @SneakyThrows
  void testUpdateGroup() {

    final var group = new Group();
    group.setName(faker.team().name());

    mockMvc.perform(post("/department")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(group)))
        .andExpect(status().isOk());

    Assertions.assertEquals(1, keycloakGroupService.getGroups().size());
    final var managedGroup = keycloakGroupService.getGroups().getFirst();

    final var request = new RenameGroupRequest();
    request.setNewGroupName(faker.team().name());

    mockMvc.perform(post("/department/" + managedGroup.getId() + ":rename")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(request.getNewGroupName())));

    Assertions.assertEquals(1, keycloakGroupService.getGroups().size());
    Assertions.assertEquals(request.getNewGroupName(),
        keycloakGroupService.getGroups().getFirst().getName());
  }

  @Test
  @SneakyThrows
  void testMoveGroup() {

    final var group_1 = dataHelper.newGroup("moveGroup_1", null);
    final var group_2 = dataHelper.newGroup("moveGroup_2", group_1);
    final var groupToMove = dataHelper.newGroup("moveGroup_3", group_2);

    mockMvc.perform(post("/department/" + groupToMove + ":move")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());

    Assertions.assertEquals(2, keycloakGroupService.getGroups()
        .stream().filter(it -> StrUtil.isBlank(it.getParentId())).count());

    mockMvc.perform(get("/department"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", equalTo(3)))
        .andExpect(jsonPath("$.[?(@.name=='moveGroup_3')].parentId", hasSize(1)))
        .andExpect(jsonPath("$.[?(@.name=='moveGroup_3')].parentId", contains(" ")));
  }

  @Test
  @SneakyThrows
  void testGetGroup() {

    final var name = faker.team().name();
    final var group = dataHelper.newGroup(name, null);

    mockMvc.perform(get("/department/" + group))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name)));
  }

  @Test
  @SneakyThrows
  void testGetGroups() {

    dataHelper.newGroup(faker.team().name(), null);
    dataHelper.newGroup(faker.team().name(), null);
    dataHelper.newGroup(faker.team().name(), null);

    mockMvc.perform(get("/department"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", equalTo(3)));
  }

  @Test
  @SneakyThrows
  void testDeleteGroup() {

    final var group = dataHelper.newGroup(faker.team().name(), null);

    mockMvc.perform(delete("/department/" + group))
        .andExpect(status().isOk());

    Assertions.assertEquals(0, keycloakGroupService.getGroups().size());
  }
}