package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizgraph.helper.VertexServiceHelper;
import com.singhand.sr.graphservice.bizgraph.model.EventItem;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEvidenceRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jVertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Feature;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex_;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EdgeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EvidenceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.FeatureRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyValueRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class JpaVertexService implements VertexService {

  private final VertexRepository vertexRepository;

  private final Neo4jVertexService neo4jVertexService;

  private final PropertyRepository propertyRepository;

  private final PropertyValueRepository propertyValueRepository;

  private final FeatureRepository featureRepository;

  private final DatasourceRepository datasourceRepository;

  private final EvidenceRepository evidenceRepository;

  private final EdgeRepository edgeRepository;

  private final VertexServiceHelper vertexServiceHelper;

  private final EntityManager entityManager;

  private final OntologyService ontologyService;

  @Autowired
  public JpaVertexService(VertexRepository vertexRepository,
      Neo4jVertexService neo4jVertexService,
      PropertyRepository propertyRepository, PropertyValueRepository propertyValueRepository,
      FeatureRepository featureRepository, DatasourceRepository datasourceRepository,
      EvidenceRepository evidenceRepository, EdgeRepository edgeRepository,
      VertexServiceHelper vertexServiceHelper,
      @Qualifier("bizEntityManager") EntityManager entityManager, OntologyService ontologyService) {

    this.vertexRepository = vertexRepository;
    this.neo4jVertexService = neo4jVertexService;
    this.propertyRepository = propertyRepository;
    this.propertyValueRepository = propertyValueRepository;
    this.featureRepository = featureRepository;
    this.datasourceRepository = datasourceRepository;
    this.evidenceRepository = evidenceRepository;
    this.edgeRepository = edgeRepository;
    this.vertexServiceHelper = vertexServiceHelper;
    this.entityManager = entityManager;
    this.ontologyService = ontologyService;
  }

  @Override
  public Optional<Vertex> getVertex(String id) {

    return vertexRepository.findById(id);
  }

  @Override
  public Page<Vertex> getVertices(String keyword, Set<String> types, boolean useEs,
      Pageable pageable) {

    if (useEs) {
      final var searchResult = Search.session(entityManager)
          .search(Vertex.class)
          .where(f -> f.bool()
              .must(f1 -> StrUtil.isBlank(keyword) ? f1.matchAll() : f1.bool()
                  // 包含匹配（基础匹配）
                  .must(f1.wildcard().field("name_keyword").matching("*" + keyword + "*"))
                  // 精确匹配最高权重
                  .should(f1.match().field("name_keyword").matching(keyword).boost(200.0f))
                  // 词项开头匹配（比如完整单词）
                  .should(f1.phrase().field("name_keyword").matching(keyword).boost(150.0f))
                  // 前缀匹配（以关键词开头）
                  .should(f1.wildcard().field("name_keyword").matching(keyword + "*").boost(100.0f))
                  // 使用simpleQueryString进行模糊搜索（~表示模糊搜索）
                  .should(f1.simpleQueryString().field("name_keyword").matching(keyword + "~")
                      .boost(50.0f))
              )
              .must(f2 -> CollUtil.isEmpty(types) ? f2.matchAll() : f2.terms()
                  .field("type_keyword")
                  .matchingAny(types))
          )
          .sort(s -> StrUtil.isBlank(keyword) ? s.field(Vertex_.CREATED_AT) : s.score());

      // 使用滚动API实现游标分页
      try (var scrollResult = searchResult.scroll(pageable.getPageSize())) {
        final var vertices = scrollResult.next().hits();
        long totalHits = searchResult.fetchTotalHitCount();
        return new PageImpl<>(vertices, pageable, totalHits);
      }
    }

    final var specifications = Specification
        .where(nameLike(keyword))
        .and(typesIn(types));

    return vertexRepository.findAll(specifications, pageable);
  }

  @Override
  public void deleteVertex(String id) {

    vertexServiceHelper.deleteVertex(id);
  }

  @Override
  public void deleteVertices(List<String> vertexIds) {

    vertexServiceHelper.deleteVertices(vertexIds);
  }

  @Override
  public Vertex newVertex(@Nonnull NewVertexRequest newVertexRequest) {

    final var name = StrUtil.trim(newVertexRequest.getName());

    final var vertex = new Vertex();
    vertex.setName(name);
    vertex.setType(newVertexRequest.getType());
    vertex.setHierarchyLevel(newVertexRequest.getHierarchyLevel());
    final var managedVertex = vertexRepository.save(vertex);

    Datasource datasource;
    if (null != newVertexRequest.getDatasourceId()) {
      datasource = getDatasource(newVertexRequest);
    } else {
      datasource = null;
    }

    neo4jVertexService.newVertex(managedVertex);

    newVertexRequest.getProps().forEach((k, v) -> {
      final var newPropertyRequest = new NewPropertyRequest();
      newPropertyRequest.setKey(k);
      newPropertyRequest.setValue(v);
      newPropertyRequest.setContent(newVertexRequest.getContent());
      newPropertyRequest.setDatasourceId(null == datasource ? null : datasource.getID());
      newPropertyRequest.setCreator(newVertexRequest.getCreator());
      newProperty(managedVertex, newPropertyRequest);
    });

    return managedVertex;
  }

  @Override
  public Vertex updateVertex(String id, String name) {

    final var vertex = getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertex.setName(name);

    final var managedVertex = vertexRepository.save(vertex);

    neo4jVertexService.updateVertex(id, name);

    return managedVertex;
  }

  @Override
  public void newProperty(@Nonnull Vertex vertex, @Nonnull NewPropertyRequest request) {

    final var property = getProperty(vertex, request.getKey())
        .orElse(new Property());
    property.setKey(request.getKey());
    vertex.addProperty(property);
    final var managedProperty = propertyRepository.save(property);

    final var valueMd5 = MD5.create().digestHex(request.getValue());
    var propertyValue = managedProperty.getValues()
        .stream()
        .filter(it -> it.getMd5().equals(valueMd5))
        .findFirst()
        .orElse(new PropertyValue());
    managedProperty.addValue(propertyValue);
    propertyValue.setValue(request.getValue());

    if (null != request.getDatasourceId()) {
      final var datasource = datasourceRepository.findById(request.getDatasourceId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));
      propertyValue.setConfidence(datasource.getConfidence());
    }
    final var managedPropertyValue = propertyValueRepository.save(propertyValue);

    addEvidence(managedPropertyValue, request);

    neo4jVertexService.newProperty(vertex, request);
  }

  @Override
  public void newProperty(@Nonnull Edge edge, @Nonnull NewPropertyRequest newPropertyRequest) {

    final var property = getProperty(edge, newPropertyRequest.getKey())
        .orElse(new Property());
    property.setKey(newPropertyRequest.getKey());
    edge.addProperty(property);
    final var managedProperty = propertyRepository.save(property);

    final var valueMd5 = MD5.create().digestHex(newPropertyRequest.getValue());
    final var propertyValue = managedProperty.getValues()
        .stream()
        .filter(it -> it.getMd5().equals(valueMd5))
        .findFirst()
        .orElse(new PropertyValue());
    managedProperty.addValue(propertyValue);
    propertyValue.setValue(newPropertyRequest.getValue());

    if (null != newPropertyRequest.getDatasourceId()) {
      final var datasource = datasourceRepository.findById(newPropertyRequest.getDatasourceId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));
      propertyValue.setConfidence(datasource.getConfidence());
    }
    final var managedPropertyValue = propertyValueRepository.save(propertyValue);

    addEvidence(managedPropertyValue, newPropertyRequest);

    neo4jVertexService.newEdgeProperty(edge.getName(), edge.getInVertex().getID(),
        edge.getOutVertex().getID(), newPropertyRequest.getKey(), newPropertyRequest.getValue());
  }

  @Override
  public void updateProperty(@Nonnull Vertex vertex, @Nonnull UpdatePropertyRequest request) {

    final var managedProperty = Optional.ofNullable(
            vertex.getProperties().get(request.getKey()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在"));

    final var oldValueMd5 = MD5.create().digestHex(request.getOldValue());

    final var oldPropertyValue = managedProperty.getValues()
        .stream()
        .filter(value -> value.getMd5().equals(oldValueMd5))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在"));

    final var newValueMd5 = MD5.create().digestHex(request.getNewValue());

    if (null != request.getDatasourceId()) {
      final var datasource = datasourceRepository.findById(request.getDatasourceId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));
      oldPropertyValue.setConfidence(datasource.getConfidence());
    }
    final var managedOldPropertyValue = propertyValueRepository.save(oldPropertyValue);

    if (oldValueMd5.equals(newValueMd5)) {
      if (oldPropertyValue.getEvidences().stream()
          .noneMatch(it -> it.getContent().equals(request.getContent()))) {
        managedOldPropertyValue.clearEvidences();
        addEvidence(managedOldPropertyValue, request);
      }
    } else {
      managedOldPropertyValue.clearEvidences();
      managedOldPropertyValue.setValue(request.getNewValue());
      final var propertyValue = propertyValueRepository.save(managedOldPropertyValue);
      addEvidence(propertyValue, request);
      neo4jVertexService.updateProperty(vertex, request);
    }
  }

  @Override
  public void updateProperty(@Nonnull Edge edge, @Nonnull UpdatePropertyRequest request) {

    final var managedProperty = Optional
        .ofNullable(edge.getProperties().get(request.getKey()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在"));

    final var oldValueMd5 = MD5.create().digestHex(request.getOldValue());
    final var oldPropertyValue = managedProperty.getValues()
        .stream()
        .filter(value -> value.getMd5().equals(oldValueMd5))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在"));

    final var newValueMd5 = MD5.create().digestHex(request.getNewValue());

    if (null != request.getDatasourceId()) {
      final var datasource = datasourceRepository.findById(request.getDatasourceId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));
      oldPropertyValue.setConfidence(datasource.getConfidence());
    }
    final var managedOldPropertyValue = propertyValueRepository.save(oldPropertyValue);

    if (oldValueMd5.equals(newValueMd5)) {
      managedOldPropertyValue.getEvidences().clear();
      addEvidence(managedOldPropertyValue, request);
    } else {
      managedOldPropertyValue.clearEvidences();
      managedOldPropertyValue.setValue(request.getNewValue());
      final var propertyValue = propertyValueRepository.save(managedOldPropertyValue);
      addEvidence(propertyValue, request);
    }
  }

  @Override
  public void deletePropertyValue(@Nonnull Vertex vertex, @Nonnull String key,
      @Nonnull String value, @Nonnull String mode) {

    final var dbProperty = Optional.ofNullable(vertex.getProperties().get(key))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在"));

    String valueMd5;
    if (mode.equals("md5")) {
      valueMd5 = value;
    } else {
      valueMd5 = MD5.create().digestHex(value);
    }

    final var dbPropertyValue = dbProperty.getValues().stream()
        .filter(it -> it.getMd5().equals(valueMd5))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在"));

    dbProperty.removeValue(dbPropertyValue);
    propertyRepository.save(dbProperty);

    if (CollUtil.isEmpty(dbProperty.getValues())) {
      vertex.getProperties().remove(key);
      dbProperty.setVertex(null);
      vertexRepository.save(vertex);
    }

    neo4jVertexService.deletePropertyValue(vertex, key, dbPropertyValue.getValue());
  }

  @Override
  public void deleteProperty(Vertex vertex, String key) {

    vertexServiceHelper.deleteProperty(vertex, key);
  }

  @Override
  public void deleteProperty(@Nonnull Edge edge, @Nonnull String key, @Nonnull String value,
      @Nonnull String mode) {

    final var dbProperty = Optional.ofNullable(edge.getProperties().get(key))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在"));

    final var valueMd5 = mode.equals("md5") ? value : MD5.create().digestHex(value);

    final var dbPropertyValue = dbProperty.getValues().stream()
        .filter(it -> it.getMd5().equals(valueMd5))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在"));

    dbProperty.getValues().remove(dbPropertyValue);
    dbPropertyValue.setProperty(null);
    propertyRepository.save(dbProperty);

    if (CollUtil.isEmpty(dbProperty.getValues())) {
      edge.getProperties().remove(key);
      dbProperty.setVertex(null);
      edgeRepository.save(edge);
    }
  }

  @Override
  public Edge newEdge(@Nonnull Vertex inVertex, @Nonnull Vertex outVertex,
      @Nonnull NewEdgeRequest request) {

    assert !inVertex.equals(outVertex);

    if (null != request.getDatasourceId()) {
      getDatasource(request);
    }

    final var oldEdge = edgeRepository.findByNameAndInVertexAndOutVertexAndScope(
        request.getName(), inVertex, outVertex, request.getScope());

    if (oldEdge.isPresent()) {
      final var edge = oldEdge.get();
      edge.getEvidences()
          .stream()
          .filter(it -> it.getDatasource().getID().equals(request.getDatasourceId()))
          .forEach(Evidence::detachAll);

      addEvidence(edge, request);
      return edge;
    }

    final var edge = new Edge();
    edge.setInVertex(inVertex);
    edge.setOutVertex(outVertex);
    edge.setScope(request.getScope());
    edge.setName(request.getName());
    request.getFeatures().forEach((k, v) -> setFeature(edge, k, v));
    final var managedEdge = edgeRepository.save(edge);

    neo4jVertexService.newEdge(request.getName(), inVertex, outVertex);

    request.getProps().forEach((k, v) -> {
      final var newPropertyRequest = new NewPropertyRequest();
      newPropertyRequest.setKey(k);
      newPropertyRequest.setValue(request.getProps().get(v));
      newPropertyRequest.setContent(request.getContent());
      newPropertyRequest.setDatasourceId(request.getDatasourceId());
      newProperty(edge, newPropertyRequest);
    });

    inVertex.getActiveEdges().add(managedEdge);
    outVertex.getPassiveEdges().add(managedEdge);

    addEvidence(edge, request);

    return managedEdge;
  }

  @Override
  public void addEvidence(@Nonnull Edge edge, @Nonnull NewEvidenceRequest newEvidenceRequest) {

    if (null == newEvidenceRequest.getDatasourceId()) {
      return;
    }

    final var datasource = getDatasource(newEvidenceRequest);
    final var evidence = new Evidence();
    evidence.setContent(newEvidenceRequest.getContent());
    edge.addEvidence(evidence);
    datasource.addEvidence(evidence);
    evidenceRepository.save(evidence);
  }

  @Override
  public void addEvidence(@Nonnull PropertyValue propertyValue,
      @Nonnull NewEvidenceRequest newEvidenceRequest) {

    if (null == newEvidenceRequest.getDatasourceId()) {
      return;
    }

    final var datasource = getDatasource(newEvidenceRequest);

    final var evidence = new Evidence();
    evidence.setContent(newEvidenceRequest.getContent());
    propertyValue.addEvidence(evidence);
    datasource.addEvidence(evidence);
    evidenceRepository.save(evidence);
  }

  @Override
  public void setFeature(@Nonnull Edge edge, @Nonnull String key, @Nonnull String value) {

    final var feature = edge.getFeatures().getOrDefault(key, new Feature());
    feature.setKey(key);
    feature.setValue(value);
    edge.addFeature(feature);
    featureRepository.save(feature);
  }

  @Override
  public void setFeature(@Nonnull PropertyValue propertyValue, @Nonnull String key,
      @Nonnull String value) {

    final var feature = propertyValue.getFeatures().getOrDefault(key, new Feature());
    feature.setKey(key);
    feature.setValue(value);
    propertyValue.addFeature(feature);
    featureRepository.save(feature);
  }

  @Override
  public Optional<Edge> getEdge(String name, Vertex inVertex, Vertex outVertex, String scope) {

    return edgeRepository.findByNameAndInVertexAndOutVertexAndScope(name, inVertex, outVertex,
        scope);
  }

  @Override
  public Optional<Property> getProperty(Vertex vertex, String key) {

    return propertyRepository.findByVertexAndKey(vertex, key);
  }

  @Override
  public Optional<Property> getProperty(Edge edge, String key) {

    return propertyRepository.findByEdgeAndKey(edge, key);
  }

  @Override
  public void deleteEdge(Edge edge) {

    vertexServiceHelper.deleteEdge(edge);
  }

  @Override
  public void updateEdge(@Nonnull Edge edge, @Nonnull UpdateEdgeRequest request) {

    request.getFeatures().forEach((k, v) -> setFeature(edge, k, v));
    final var managedNewEdge = edgeRepository.save(edge);
    managedNewEdge.clearEvidences();
    addEvidence(edge, request);

    neo4jVertexService.updateEdge(request.getOldName(), request.getNewName(),
        edge.getInVertex(), edge.getOutVertex());
  }

  @Override
  public void batchDeleteVertex(Set<String> types) {

    vertexServiceHelper.batchDeleteVertex(types);
  }

  @Override
  public void batchUpdateVertex(String oldType, String newType) {

    vertexServiceHelper.batchUpdateVertex(oldType, newType);
  }

  @Override
  public void batchUpdateVertexProperty(String vertexType, String oldKey, String newKey) {

    vertexServiceHelper.batchUpdateVertexProperty(vertexType, oldKey, newKey);
  }

  @Override
  public void batchDeleteVertexProperty(String vertexType, String key) {

    vertexServiceHelper.batchDeleteVertexProperty(vertexType, key);
  }

  @Override
  public void batchUpdateVertexEdge(String name, String newName, String inVertexType,
      String outVertexType) {

    vertexServiceHelper.batchUpdateVertexEdge(name, newName, inVertexType, outVertexType);
  }

  @Override
  public void batchUpdateVertexEdge(String name, String newName) {

    vertexServiceHelper.batchUpdateVertexEdge(name, newName);
  }

  @Override
  public void batchDeleteVertexEdge(String name, String inVertexType, String outVertexType) {

    vertexServiceHelper.batchDeleteVertexEdge(name, inVertexType, outVertexType);
  }

  @Override
  public void batchDeleteVertexEdge(String name) {

    vertexServiceHelper.batchDeleteVertexEdge(name);
  }

  @Override
  public Page<Evidence> getEvidences(Vertex vertex, String key, String value, String mode,
      Pageable pageable) {

    final var property = getProperty(vertex, key)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在"));
    final var propertyValue = getPropertyValue(property, value, mode)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在"));
    return evidenceRepository.findByPropertyValue(propertyValue, pageable);
  }

  @Override
  public Page<Evidence> getEvidences(Edge edge, String key, String value, String mode,
      Pageable pageable) {

    return evidenceRepository.findByEdge(edge, pageable);
  }

  @Override
  public Page<Edge> getEdges(String keyword, String name, Pageable pageable) {

    return edgeRepository.findAll(Specification.where(vertexNameLike(keyword)
        .and(edgeNameLike(name))), pageable);
  }

  @Override
  public Long countEdges() {

    return edgeRepository.count();
  }

  @Override
  public Long countVertices(String level) {

    return vertexRepository.count(Specification.where(levelIs(level)));
  }

  @Override
  public Collection<PropertyValue> getPropertyValues(Vertex vertex) {

    return propertyValueRepository.findDistinctByProperty_Vertex(vertex);
  }

  @Override
  public Page<EventItem> getEventTrend(String id, Pageable pageable) {

    final var vertex = getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在！"));

    final var eventTypes = ontologyService.getAllSubOntologies(Set.of("事件"));
    final var edges = edgeRepository
        .findByInVertex_IDAndOutVertex_TypeIn(vertex.getID(), eventTypes);

    if (CollUtil.isEmpty(edges)) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    final var eventItems = edges.stream()
        .map(it -> new EventItem(it.getOutVertex()))
        .sorted(Comparator.comparing(EventItem::getTime))
        .skip(pageable.getOffset())
        .limit(pageable.getPageSize())
        .toList();

    return new PageImpl<>(eventItems, pageable, edges.size());
  }

  private static @Nonnull Specification<Vertex> levelIs(String level) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(level)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.equal(root.get(Vertex_.HIERARCHY_LEVEL), level);
    };
  }

  private static @Nonnull Specification<Edge> edgeNameLike(String name) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(name)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.like(root.get(Edge_.NAME), "%" + name.trim() + "%");
    };
  }

  private static @Nonnull Specification<Edge> vertexNameLike(String keyword) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(keyword)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.or(
          criteriaBuilder
              .like(root.get(Edge_.IN_VERTEX).get(Vertex_.NAME), "%" + keyword.trim() + "%"),
          criteriaBuilder
              .like(root.get(Edge_.OUT_VERTEX).get(Vertex_.NAME), "%" + keyword.trim() + "%")
      );
    };
  }

  private Datasource getDatasource(@Nonnull NewEvidenceRequest newEvidenceRequest) {

    return datasourceRepository.findById(newEvidenceRequest.getDatasourceId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));
  }

  private static @Nonnull Specification<Vertex> typesIn(Set<String> types) {

    return (root, query, criteriaBuilder) -> {
      if (CollUtil.isEmpty(types)) {
        return criteriaBuilder.and();
      }
      return root.get(Vertex_.TYPE).in(types);
    };
  }

  private static @Nonnull Specification<Vertex> nameLike(String keyword) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(keyword)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.like(root.get(Vertex_.NAME), "%" + keyword.trim() + "%");
    };
  }
}
