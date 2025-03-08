package com.singhand.sr.graphservice.bizservice.unit.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.DeletePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import com.singhand.sr.graphservice.bizservice.BaseTestEnvironment;
import com.singhand.sr.graphservice.testenvironments.mock.MockOntology;
import com.singhand.sr.graphservice.testenvironments.mock.MockOntologyProperty;
import java.util.HashSet;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
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
  void testUpdateOntology() {

    final var name = faker.name().name();
    final var name_2 = faker.name().name();

    mockMvc.perform(post("/ontology")
            .param("name", name))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name)));

    mockMvc.perform(post("/ontology")
            .param("name", name_2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name_2)));

    final var ontology = ontologyRepository.findByName(name).orElseThrow();

    final var name_3 = faker.name().name();

    mockMvc.perform(put("/ontology/" + ontology.getID())
            .param("name", name_2))
        .andExpect(status().isConflict());

    mockMvc.perform(put("/ontology/" + ontology.getID())
            .param("name", name))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name)));

    mockMvc.perform(put("/ontology/" + ontology.getID())
            .param("name", name_3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(name_3)));
  }

  @Test
  @SneakyThrows
  void testDeleteOntology() {

    final var properties = Set.of(faker.lorem().sentence(), faker.lorem().sentence());

    final var ontology = dataHelper.newOntology("test_ontology_01", null, properties);

    final var ontology_2 = dataHelper
        .newOntology("test_ontology_02", "test_ontology_01", properties);

    final var ontology_3 = dataHelper
        .newOntology("test_ontology_03", "test_ontology_02", properties);

    final var ontology_4 = dataHelper
        .newOntology("test_ontology_04", "test_ontology_03", properties);

    mockMvc.perform(delete("/ontology/" + ontology.getID()))
        .andExpect(status().isOk());

    final var managedOntology = ontologyRepository.findById(ontology.getID()).orElse(null);
    final var managedOntology_2 = ontologyRepository.findById(ontology_2.getID()).orElse(null);
    final var managedOntology_3 = ontologyRepository.findById(ontology_3.getID()).orElse(null);
    final var managedOntology_4 = ontologyRepository.findById(ontology_4.getID()).orElse(null);
    Assertions.assertNull(managedOntology);
    Assertions.assertNull(managedOntology_2);
    Assertions.assertNull(managedOntology_3);
    Assertions.assertNull(managedOntology_4);
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "test_ontology_01")
  void testNewOntologyProperty() {

    final var ontology = ontologyRepository.findByName("test_ontology_01").orElseThrow();

    final var propertyRequest = new NewOntologyPropertyRequest();
    propertyRequest.setName(faker.lorem().sentence());
    propertyRequest.setType("STRING");

    mockMvc.perform(post("/ontology/" + ontology.getID() + "/property")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(JSONUtil.toJsonStr(propertyRequest)))
        .andExpect(status().isOk());

    mockMvc.perform(post("/ontology/" + ontology.getID() + "/property")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(JSONUtil.toJsonStr(propertyRequest)))
        .andExpect(status().isConflict());

    mockMvc.perform(get("/ontology/" + ontology.getID() + "/property")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", equalTo(1)))
        .andExpect(jsonPath("$.content[0].name", equalTo(propertyRequest.getName())));
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "test_ontology_01", properties = {
      @MockOntologyProperty(name = "test_property_01", type = "STRING")
  })
  void testUpdateOntologyProperty() {

    final var ontology = ontologyRepository.findByName("test_ontology_01").orElseThrow();

    final var propertyRequest = new UpdateOntologyPropertyRequest();
    propertyRequest.setOldName("test_property_01");
    propertyRequest.setNewName(faker.lorem().sentence());
    propertyRequest.setType(faker.lorem().sentence());

    mockMvc.perform(put("/ontology/" + ontology.getID() + "/property")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(JSONUtil.toJsonStr(propertyRequest)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/ontology/" + ontology.getID() + "/property")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", equalTo(1)))
        .andExpect(jsonPath("$.content[0].name", equalTo(propertyRequest.getNewName())))
        .andExpect(jsonPath("$.content[0].type", equalTo(propertyRequest.getType())));
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "test_ontology_01", properties = {
      @MockOntologyProperty(name = "test_property_01", type = "STRING"),
      @MockOntologyProperty(name = "test_property_02", type = "STRING"),
      @MockOntologyProperty(name = "test_property_03", type = "STRING"),
      @MockOntologyProperty(name = "test_property_04", type = "STRING")
  })
  void testDeleteOntologyProperty() {

    final var ids = new HashSet<Long>();

    final var ontology = ontologyRepository.findByName("test_ontology_01").orElseThrow();

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var managedOntology = ontologyRepository.findById(ontology.getID()).orElseThrow();
      Assertions.assertEquals(4, managedOntology.getProperties().size());
      managedOntology.getProperties().forEach(it -> ids.add(it.getID()));
    });

    final var propertyRequest = new DeletePropertyRequest();
    propertyRequest.setPropertyIds(ids);

    mockMvc.perform(delete("/ontology/" + ontology.getID() + "/property")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(JSONUtil.toJsonStr(propertyRequest)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/ontology/" + ontology.getID() + "/property")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", equalTo(0)));
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
