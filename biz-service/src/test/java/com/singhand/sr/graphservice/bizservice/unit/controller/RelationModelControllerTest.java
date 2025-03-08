package com.singhand.sr.graphservice.bizservice.unit.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import com.singhand.sr.graphservice.bizservice.BaseTestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@Slf4j
@WithMockUser(username = "admin")
public class RelationModelControllerTest extends BaseTestEnvironment {

  @Autowired
  private RelationModelRepository relationModelRepository;

  @Test
  @SneakyThrows
  void testNewRelationModel() {

    final var name = faker.name().name();

    mockMvc.perform(post("/relationModel")
            .param("name", name))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name)));

    mockMvc.perform(post("/relationModel")
            .param("name", name))
        .andExpect(status().isConflict());
  }

  @Test
  @SneakyThrows
  void testUpdateRelationModel() {

    final var relationModel = dataHelper.newRelationModel("test_update_relation_model_1");

    final var relationModel_2 = dataHelper.newRelationModel("test_update_relation_model_2");

    final var name = faker.name().name();

    mockMvc.perform(put("/relationModel/" + relationModel.getID())
            .param("name", name))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name)));

    mockMvc.perform(put("/relationModel/" + relationModel_2.getID())
            .param("name", name))
        .andExpect(status().isConflict());
  }

  @Test
  @SneakyThrows
  void testDeleteRelationModel() {

    final var relationModel = dataHelper.newRelationModel("test_delete_relation_model_1");

    mockMvc.perform(delete("/relationModel/" + relationModel.getID()))
        .andExpect(status().isOk());

    Assertions.assertFalse(relationModelRepository.existsByName(relationModel.getName()));
  }
}
