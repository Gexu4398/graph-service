package com.singhand.sr.graphservice.bizservice.unit.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.hutool.core.collection.CollUtil;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import com.singhand.sr.graphservice.bizservice.BaseTestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@Slf4j
@WithMockUser(username = "admin")
public class OntologyControllerTest extends BaseTestEnvironment {

  @Autowired
  private OntologyRepository ontologyRepository;

  @Autowired
  private OntologyNodeRepository ontologyNodeRepository;

  @Test
  @SneakyThrows
  void testNewOntology() {

    final var name = faker.name().name();
    final var name_2 = faker.name().name();

    mockMvc.perform(post("/ontology")
            .param("name", name))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name)));

    mockMvc.perform(post("/ontology")
            .param("name", name))
        .andExpect(status().isConflict());

    mockMvc.perform(post("/ontology")
            .param("name", name_2)
            .param("parentId", "9999"))
        .andExpect(status().isNotFound());

    final var ontology = ontologyRepository.findByName(name).orElseThrow();

    mockMvc.perform(post("/ontology")
            .param("name", name_2)
            .param("parentId", String.valueOf(ontology.getID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name_2)));

    final var ontologyNode = ontologyNodeRepository.findById(ontology.getID()).orElseThrow();

    Assertions.assertEquals(1, ontologyNode.getChildren().size());
    Assertions.assertEquals(CollUtil.getFirst(ontologyNode.getChildren()).getName(), name_2);
  }
}
