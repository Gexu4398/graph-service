package com.singhand.sr.graphservice.testenvironments.helper;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.RelationModelService;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
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
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.DatasourceContent;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceContentRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

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

  private final RelationModelService relationModelService;

  private final DatasourceRepository datasourceRepository;

  private final VertexService vertexService;

  private final DatasourceContentRepository datasourceContentRepository;

  @Autowired
  public DataHelper(KeycloakUserService keycloakUserService,
      KeycloakGroupService keycloakGroupService, KeycloakRoleService keycloakRoleService,
      UserEntityRepository userEntityRepository, OntologyService ontologyService,
      OntologyRepository ontologyRepository, RelationModelService relationModelService,
      DatasourceRepository datasourceRepository, VertexService vertexService,
      DatasourceContentRepository datasourceContentRepository) {

    this.keycloakUserService = keycloakUserService;
    this.keycloakGroupService = keycloakGroupService;
    this.keycloakRoleService = keycloakRoleService;
    this.userEntityRepository = userEntityRepository;
    this.ontologyService = ontologyService;
    this.ontologyRepository = ontologyRepository;
    this.relationModelService = relationModelService;
    this.datasourceRepository = datasourceRepository;
    this.vertexService = vertexService;
    this.datasourceContentRepository = datasourceContentRepository;
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

  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Ontology newOntology(String name, String parent, Set<String> properties) {

    if (StrUtil.isNotBlank(parent)) {
      final var parentOntology = ontologyRepository.findByName(parent)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      return ontologyService.newOntology(name, parentOntology.getID());
    }
    final var ontology = ontologyService.newOntology(name, null);

    properties.forEach(it -> {
      final var request = new NewOntologyPropertyRequest();
      request.setName(it);
      request.setType("STRING");
      ontologyService.newOntologyProperty(ontology, request);
    });

    return ontologyRepository.findById(ontology.getID()).orElseThrow();
  }

  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void newOntologyProperty(Ontology ontology, String name, String type) {

    final var request = new NewOntologyPropertyRequest();
    request.setName(name);
    request.setType(type);
    ontologyService.newOntologyProperty(ontology, request);
  }

  @SneakyThrows
  @Transactional("bizTransactionManager")
  public RelationModel newRelationModel(String name) {

    return relationModelService.newRelationModel(name);
  }

  public Datasource newDatasource(String name) {

    return newDatasource(name, "");
  }

  public Datasource newDatasource(String name, String text, String html, String url) {

    final var datasource = new Datasource();
    datasource.setTitle(NAME_PREFIX + name);
    datasource.setUrl(url);
    datasource.setCreator("admin");

    final var managedDatasource = datasourceRepository.save(datasource);

    final var datasourceContent = new DatasourceContent();
    datasourceContent.setID(managedDatasource.getID());
    datasourceContent.setText(text);
    datasourceContent.setHtml(html);
    datasourceContentRepository.save(datasourceContent);

    return managedDatasource;
  }

  public Datasource newDatasource(String name, String text) {

    final var datasource = new Datasource();
    datasource.setTitle(NAME_PREFIX + name);
    datasource.setCreator("admin");

    final var managedDatasource = datasourceRepository.save(datasource);

    final var datasourceContent = new DatasourceContent();
    datasourceContent.setID(managedDatasource.getID());
    datasourceContent.setText(text);
    datasourceContentRepository.save(datasourceContent);

    return datasource;
  }

  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Vertex newVertex(String name, String type) {

    final var request = new NewVertexRequest();
    request.setName(name);
    request.setType(type);

    return vertexService.newVertex(request);
  }

  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Vertex newVertex(String name, String type, Map<String, String> props) {

    final var request = new NewVertexRequest();
    request.setName(name);
    request.setType(type);
    request.setProps(props);

    return vertexService.newVertex(request);
  }

  @Transactional("bizTransactionManager")
  public void newProperty(Vertex vertex, String key, String value, String evidence, Datasource datasource) {

    final var newPropertyRequest = new NewPropertyRequest();
    newPropertyRequest.setKey(key);
    newPropertyRequest.setValue(value);
    newPropertyRequest.setContent(evidence);
    newPropertyRequest.setCreator("admin");
    Optional.ofNullable(datasource)
        .ifPresent(it -> newPropertyRequest.setDatasourceId(datasource.getID()));
    vertexService.newProperty(vertex, newPropertyRequest);
  }

  @Transactional("bizTransactionManager")
  public void newOntologyRelation(String name, Ontology inOntology, Ontology outOntology) {

    ontologyService.newRelation(name, inOntology, outOntology);
  }

  @Transactional("bizTransactionManager")
  public Edge newEdge(Vertex inVertex, Vertex outVertex, String name) {

    return newEdge(inVertex, outVertex, name, "", Map.of(), null, "default");
  }

  @Transactional("bizTransactionManager")
  public Edge newEdge(Vertex inVertex, Vertex outVertex, String name,
      String evidence, Map<String, String> features) {

    return newEdge(inVertex, outVertex, name, evidence, features, null, "default");
  }

  @Transactional("bizTransactionManager")
  public Edge newEdge(Vertex inVertex, Vertex outVertex, String name,
      String evidence, Map<String, String> features, Datasource datasource, String scope) {

    final var newVertexEdgeRequest = new NewEdgeRequest();
    newVertexEdgeRequest.setName(name);
    newVertexEdgeRequest.setScope(scope);
    newVertexEdgeRequest.setContent(evidence);
    newVertexEdgeRequest.setFeatures(features);
    if (datasource != null) {
      newVertexEdgeRequest.setDatasourceId(datasource.getID());
    }
    newVertexEdgeRequest.setCreator("admin");
    return vertexService.newEdge(inVertex, outVertex, newVertexEdgeRequest);
  }
}
