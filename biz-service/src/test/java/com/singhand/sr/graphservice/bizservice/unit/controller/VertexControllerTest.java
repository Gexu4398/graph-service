package com.singhand.sr.graphservice.bizservice.unit.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyValueRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.VertexNodeRepository;
import com.singhand.sr.graphservice.bizservice.BaseTestEnvironment;
import com.singhand.sr.graphservice.testenvironments.mock.MockDataSource;
import com.singhand.sr.graphservice.testenvironments.mock.MockOntology;
import com.singhand.sr.graphservice.testenvironments.mock.MockOntologyProperty;
import java.util.List;
import java.util.Map;
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
public class VertexControllerTest extends BaseTestEnvironment {

  @Autowired
  private VertexRepository vertexRepository;

  @Autowired
  private VertexNodeRepository vertexNodeRepository;

  @Autowired
  @Qualifier("bizTransactionManager")
  private PlatformTransactionManager bizTransactionManager;

  @Autowired
  private PropertyRepository propertyRepository;

  @Autowired
  private PropertyValueRepository propertyValueRepository;

  @Autowired
  private DatasourceRepository datasourceRepository;

  @Test
  @SneakyThrows
  @MockOntology(name = "testNewVertex")
  public void testNewVertex() {

    final var request = new NewVertexRequest();
    request.setName(faker.name().name());
    request.setType("testNewVertex");

    mockMvc.perform(post("/vertex")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(request.getName())))
        .andExpect(jsonPath("$.type", equalTo(request.getType())));

    final var exists = vertexNodeRepository
        .findByNameAndType(request.getName(), request.getType()).isPresent();

    Assertions.assertTrue(exists);
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "testNewVertex", properties = {
      @MockOntologyProperty(name = "age", type = "string"),
      @MockOntologyProperty(name = "height", type = "string"),
      @MockOntologyProperty(name = "weight", type = "string")
  })
  public void testNewVertex_With_Property() {

    final var request = new NewVertexRequest();
    request.setName(faker.name().name());
    request.setType("testNewVertex");

    request.setProps(Map.of("age", "18",
        "height", "185",
        "weight", "140"));

    mockMvc.perform(post("/vertex")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(request.getName())))
        .andExpect(jsonPath("$.type", equalTo(request.getType())));

    final var vertexNode = vertexNodeRepository
        .findByNameAndType(request.getName(), request.getType())
        .orElse(null);

    Assertions.assertNotNull(vertexNode);
    Assertions.assertEquals(3, vertexNode.getProperties().size());

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var vertex = vertexRepository.findById(vertexNode.getId()).orElseThrow();
      Assertions.assertEquals(3, vertex.getProperties().size());
    });
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "testUpdateVertex")
  public void testUpdateVertex() {

    final var vertex = dataHelper.newVertex(faker.name().name(), "testUpdateVertex");

    final var newName = faker.name().name();

    mockMvc.perform(put("/vertex/" + vertex.getID())
            .param("name", newName))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", equalTo(newName)));

    final var exists = vertexNodeRepository
        .findByNameAndType(newName, vertex.getType()).isPresent();

    Assertions.assertTrue(exists);
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "testDeleteVertex", properties = {
      @MockOntologyProperty(name = "age", type = "string"),
      @MockOntologyProperty(name = "height", type = "string"),
      @MockOntologyProperty(name = "weight", type = "string")
  })
  public void testDeleteVertex() {

    final var props = Map.of("age", "18",
        "height", "185",
        "weight", "140");

    final var vertex = dataHelper.newVertex(faker.name().name(), "testDeleteVertex", props);

    mockMvc.perform(delete("/vertex/" + vertex.getID()))
        .andExpect(status().isOk());

    final var exists = vertexNodeRepository
        .findByNameAndType(vertex.getName(), vertex.getType()).isPresent();

    Assertions.assertFalse(exists);

    final var values = propertyValueRepository.findByProperty_Vertex_ID(vertex.getID());
    final var properties = propertyRepository.findByVertex_ID(vertex.getID());
    Assertions.assertEquals(0, values.size());
    Assertions.assertEquals(0, properties.size());
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "testNewProperty", properties = {
      @MockOntologyProperty(name = "age", type = "string")
  })
  public void testNewProperty() {

    final var vertex = dataHelper.newVertex(faker.name().name(), "testNewProperty");

    final var request = new NewPropertyRequest();
    request.setValue(faker.lorem().paragraph());

    mockMvc.perform(post("/vertex/" + vertex.getID() + "/property")
            .param("key", "age")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk());

    final var propertyValue = propertyValueRepository.findByProperty_Vertex_IDAndProperty_KeyAndMd5(
        vertex.getID(), "age", MD5.create().digestHex(request.getValue())).orElse(null);

    Assertions.assertNotNull(propertyValue);
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "testNewProperty", properties = {
      @MockOntologyProperty(name = "age", type = "string")
  })
  @MockDataSource(title = "datasource_1")
  public void testNewProperty_with_datasource() {

    final var vertex = dataHelper.newVertex(faker.name().name(), "testNewProperty");

    final var datasource = datasourceRepository.findFirstByTitle("datasource_1").orElseThrow();

    final var request = new NewPropertyRequest();
    request.setValue(faker.lorem().paragraph());
    request.setContent(faker.lorem().paragraph());
    request.setDatasourceId(datasource.getID());

    mockMvc.perform(post("/vertex/" + vertex.getID() + "/property")
            .param("key", "age")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk());

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var propertyValue = propertyValueRepository.findByProperty_Vertex_IDAndProperty_KeyAndMd5(
          vertex.getID(), "age", MD5.create().digestHex(request.getValue())).orElse(null);

      Assertions.assertNotNull(propertyValue);
      Assertions.assertEquals(1, propertyValue.getEvidences().size());

      final var evidence = CollUtil.getFirst(propertyValue.getEvidences());
      Assertions.assertEquals(evidence.getContent(), request.getContent());

      final var evidenceDatasource = evidence.getDatasource();
      Assertions.assertEquals(datasource.getID(), evidenceDatasource.getID());
    });
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "testUpdateProperty", properties = {
      @MockOntologyProperty(name = "age", type = "string")
  })
  public void testUpdateProperty() {

    final var vertex = dataHelper.newVertex(faker.name().name(), "testUpdateProperty",
        Map.of("age", "18"));

    final var request = new UpdatePropertyRequest();
    request.setKey("age");
    request.setOldValue("18");
    request.setNewValue(faker.dog().age());

    mockMvc.perform(put("/vertex/" + vertex.getID() + "/property")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(List.of(request))))
        .andExpect(status().isOk());

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var propertyValue = propertyValueRepository.findByProperty_Vertex_IDAndProperty_KeyAndMd5(
          vertex.getID(), "age", MD5.create().digestHex(request.getNewValue())).orElse(null);

      Assertions.assertNotNull(propertyValue);
    });
  }

  @Test
  @SneakyThrows
  @MockOntology(name = "testUpdateProperty", properties = {
      @MockOntologyProperty(name = "age", type = "string")
  })
  @MockDataSource(title = "datasource_1")
  @MockDataSource(title = "datasource_2")
  public void testUpdateProperty_with_datasource() {

    final var vertex = dataHelper.newVertex(faker.name().name(), "testUpdateProperty",
        Map.of("age", "18"));

    final var datasource = datasourceRepository.findFirstByTitle("datasource_1").orElseThrow();
    final var datasource_2 = datasourceRepository.findFirstByTitle("datasource_2").orElseThrow();

    final var request = new UpdatePropertyRequest();
    request.setKey("age");
    request.setOldValue("18");
    request.setNewValue(faker.dog().age());
    request.setDatasourceId(datasource.getID());
    request.setContent(faker.lorem().paragraph());

    mockMvc.perform(put("/vertex/" + vertex.getID() + "/property")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(List.of(request))))
        .andExpect(status().isOk());

    request.setOldValue(request.getNewValue());
    request.setNewValue(faker.dog().age());
    request.setDatasourceId(datasource_2.getID());
    request.setContent(faker.lorem().paragraph());

    mockMvc.perform(put("/vertex/" + vertex.getID() + "/property")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(List.of(request))))
        .andExpect(status().isOk());

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var propertyValue = propertyValueRepository.findByProperty_Vertex_IDAndProperty_KeyAndMd5(
          vertex.getID(), "age", MD5.create().digestHex(request.getNewValue())).orElse(null);

      Assertions.assertNotNull(propertyValue);
      Assertions.assertEquals(1, propertyValue.getEvidences().size());

      final var evidence = CollUtil.getFirst(propertyValue.getEvidences());
      Assertions.assertEquals(evidence.getContent(), request.getContent());

      final var evidenceDatasource = evidence.getDatasource();
      Assertions.assertEquals(datasource_2.getID(), evidenceDatasource.getID());
    });
  }

  @SneakyThrows
  @Test
  @MockOntology(name = "testDeleteProperty", properties = {
      @MockOntologyProperty(name = "age", type = "string")
  })
  void testDeleteProperty() {

    final var vertex = dataHelper.newVertex(faker.name().name(), "testDeleteProperty");
    final var value = faker.dog().age();
    final var evidence = faker.lorem().sentence();
    final var datasource = dataHelper.newDatasource("testDeleteProperty");
    dataHelper.newProperty(vertex, "age", value, evidence, false, datasource);

    mockMvc.perform(delete("/vertex/" + vertex.getID() + "/property")
            .param("key", "age")
            .param("value", MD5.create().digestHex(value))
            .param("mode","md5"))
        .andExpect(status().isOk());

    new TransactionTemplate(bizTransactionManager).executeWithoutResult(status -> {
      final var managedVertex = vertexRepository.findById(vertex.getID()).orElseThrow();
      Assertions.assertTrue(managedVertex.getProperties().isEmpty());
    });
  }
}
