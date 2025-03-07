package com.singhand.sr.graphservice.bizservice.unit.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.hutool.core.collection.CollUtil;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import com.singhand.sr.graphservice.bizservice.BaseTestEnvironment;
import com.singhand.sr.graphservice.testenvironments.mock.MockOntology;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@WithMockUser(username = "admin")
public class OntologyControllerTest extends BaseTestEnvironment {

  @Autowired
  private OntologyRepository ontologyRepository;

  @Autowired
  private OntologyNodeRepository ontologyNodeRepository;

  @Autowired
  @Qualifier("bizTransactionManager")
  private PlatformTransactionManager bizTransactionManager;

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

  @Test
  @SneakyThrows
  @MockOntology(name = "test_ontology_01")
  void testNewOntologyProperty() {

    final var ontology = ontologyRepository.findByName("test_ontology_01").orElseThrow();

    final var character = faker.lorem().sentence();

    mockMvc.perform(post("/ontology/" + ontology.getID() + "/property")
            .param("propertyName", character))
        .andExpect(status().isOk());

    mockMvc.perform(post("/ontology/" + ontology.getID() + "/property")
            .param("propertyName", character))
        .andExpect(status().isConflict());

    mockMvc.perform(get("/ontology/" + ontology.getID() + "/property")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", equalTo(1)))
        .andExpect(jsonPath("$.content[0].name", equalTo(character)));
  }

  @Test
  @SneakyThrows
  void testGetTree() {

    final var ontology = dataHelper.newOntology("test_ontology_01", null);
    dataHelper.newOntology("test_ontology_02", null);

    dataHelper.newOntology("test_ontology_03", "test_ontology_01");
    dataHelper.newOntology("test_ontology_04", "test_ontology_03");
    dataHelper.newOntology("test_ontology_05", "test_ontology_04");

    dataHelper.newOntology("test_ontology_06", "test_ontology_02");

    mockMvc.perform(get("/ontology/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", equalTo(2)));

    mockMvc.perform(get("/ontology/tree")
            .param("id", String.valueOf(ontology.getID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", equalTo(1)));

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var managedOntology = ontologyRepository.findById(ontology.getID()).orElseThrow();
      Assertions.assertEquals(1, managedOntology.getChildren().size());
    });
  }
}
