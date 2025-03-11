package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEvidenceRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jVertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Feature;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex_;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EvidenceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.FeatureRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyValueRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.JoinType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class JpaVertexService implements VertexService {

  private final VertexRepository vertexRepository;

  private final Neo4jVertexService neo4jVertexService;

  private final PlatformTransactionManager bizTransactionManager;

  private final PropertyRepository propertyRepository;

  private final PropertyValueRepository propertyValueRepository;

  private final FeatureRepository featureRepository;

  private final DatasourceRepository datasourceRepository;

  private final EvidenceRepository evidenceRepository;

  private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  @Autowired
  public JpaVertexService(VertexRepository vertexRepository,
      Neo4jVertexService neo4jVertexService,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager,
      PropertyRepository propertyRepository, PropertyValueRepository propertyValueRepository,
      FeatureRepository featureRepository, DatasourceRepository datasourceRepository,
      EvidenceRepository evidenceRepository) {

    this.vertexRepository = vertexRepository;
    this.neo4jVertexService = neo4jVertexService;
    this.bizTransactionManager = bizTransactionManager;
    this.propertyRepository = propertyRepository;
    this.propertyValueRepository = propertyValueRepository;
    this.featureRepository = featureRepository;
    this.datasourceRepository = datasourceRepository;
    this.evidenceRepository = evidenceRepository;
  }

  @Override
  public Optional<Vertex> getVertex(String id) {

    return vertexRepository.findById(id);
  }

  @Override
  public Page<Vertex> getVertices(String keyword, Set<String> types, Map<String, String> keyValues,
      Pageable pageable) {

    var specifications = Specification
        .where(nameLike(keyword))
        .and(typesIn(types));
    if (CollUtil.isNotEmpty(keyValues)) {
      specifications = addKeyValueSpecifications(keyValues, specifications);
    }

    return vertexRepository.findAll(specifications, pageable);
  }

  @Override
  public Vertex newVertex(@Nonnull NewVertexRequest request) {

    final var name = StrUtil.trim(request.getName());

    final var vertex = new Vertex();
    vertex.setName(name);
    vertex.setType(request.getType());

    final var managedVertex = vertexRepository.save(vertex);

    neo4jVertexService.newVertex(managedVertex);

    Datasource datasource;
    if (null != request.getDatasourceId()) {
      datasource = getDatasource(request);
    } else {
      datasource = null;
    }

    request.getProps().forEach((k, v) -> {
      final var newPropertyRequest = new NewPropertyRequest();
      newPropertyRequest.setKey(k);
      newPropertyRequest.setValue(v);
      newPropertyRequest.setContent(request.getContent());
      newPropertyRequest.setVerified(request.isVerified());
      newPropertyRequest.setChecked(request.isChecked());
      newPropertyRequest.setDatasourceId(null == datasource ? null : datasource.getID());
      newPropertyRequest.setCreator(request.getCreator());
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
    final var managedPropertyValue = propertyValueRepository.save(propertyValue);

    if (request.getVerified()) {
      setVerified(vertex, request.getKey(), request.getValue());
    }

    setFeature(propertyValue, "checked", request.getChecked().toString());

    if (null != request.getDatasourceId()) {
      addEvidence(managedPropertyValue, request);
    }

    neo4jVertexService.newProperty(vertex, request);
  }

  @Override
  public void updateProperty(@Nonnull Vertex vertex, @Nonnull UpdatePropertyRequest request) {

    final var managedProperty = Optional.ofNullable(
            vertex.getProperties().get(request.getKey()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在！"));

    final var oldValueMd5 = MD5.create().digestHex(request.getOldValue());

    final var managedOldPropertyValue = managedProperty.getValues()
        .stream()
        .filter(value -> value.getMd5().equals(oldValueMd5))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在！"));

    final var newValueMd5 = MD5.create().digestHex(request.getNewValue());

    // 若新值与旧值相同，且不包含新证据，视为仅更新证据
    if (oldValueMd5.equals(newValueMd5)) {
      if (managedOldPropertyValue.getEvidences().stream()
          .noneMatch(it -> it.getContent().equals(request.getContent()))) {
        managedOldPropertyValue.clearEvidences();
        addEvidence(managedOldPropertyValue, request);
      }
      setFeature(managedOldPropertyValue, "checked", request.getChecked().toString());
    } else {
      managedOldPropertyValue.clearEvidences();
      managedOldPropertyValue.setValue(request.getNewValue());
      final var propertyValue = propertyValueRepository.save(managedOldPropertyValue);

      neo4jVertexService.updateProperty(vertex, request);
      addEvidence(propertyValue, request);
      setFeature(propertyValue, "checked", request.getChecked().toString());
    }

    if (request.getVerified()) {
      setVerified(vertex, request.getKey(), request.getNewValue());
    }
  }

  @Override
  public void deleteProperty(@Nonnull Vertex vertex, @Nonnull String key, @Nonnull String value,
      @Nonnull String mode) {

    final var dbProperty = Optional.ofNullable(vertex.getProperties().get(key))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在！"));

    String valueMd5;
    if (mode.equals("md5")) {
      valueMd5 = value;
    } else {
      valueMd5 = MD5.create().digestHex(value);
    }

    final var dbPropertyValue = dbProperty.getValues().stream()
        .filter(it -> it.getMd5().equals(valueMd5))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在！"));

    dbProperty.removeValue(dbPropertyValue);
    propertyRepository.save(dbProperty);

    if (CollUtil.isEmpty(dbProperty.getValues())) {
      vertex.getProperties().remove(key);
      dbProperty.setVertex(null);
      vertexRepository.save(vertex);
    }

    neo4jVertexService.deleteProperty(vertex, key, dbPropertyValue.getValue());
  }

  @Override
  public Optional<Property> getProperty(Vertex vertex, String key) {

    return propertyRepository.findByVertexAndKey(vertex, key);
  }

  @Override
  public void setVerified(Vertex vertex, String key, String value) {

    final var property = getProperty(vertex, key)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在！"));
    final var propertyValue = getPropertyValue(property, value)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在！"));
    setFeature(propertyValue, "verified", "true");
    property.getValues().stream()
        .filter(it -> it != propertyValue)
        .forEach(it -> setFeature(it, "verified", "false"));
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
  public void addEvidence(@Nonnull PropertyValue propertyValue,
      @Nonnull NewEvidenceRequest newEvidenceRequest) {

    final var managedDatasource = getDatasource(newEvidenceRequest);

    final var evidence = new Evidence();
    evidence.setContent(newEvidenceRequest.getContent());
    propertyValue.addEvidence(evidence);
    managedDatasource.addEvidence(evidence);
    evidenceRepository.save(evidence);
  }

  private Datasource getDatasource(@Nonnull NewEvidenceRequest newEvidenceRequest) {

    return datasourceRepository.findById(newEvidenceRequest.getDatasourceId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在！"));
  }

  private void updateVertices(List<String> vertexIds, String newType) {

    final var vertices = vertexRepository.findAllById(vertexIds);

    final var managedVertices = new HashSet<>(vertices);

    managedVertices.forEach(it -> {
      it.setType(newType);
      vertexRepository.save(it);
    });

    neo4jVertexService.updateVertices(vertexIds, newType);
  }

  @Override
  public void deleteVertex(String vertexId) {

    getVertex(vertexId).ifPresent(vertex -> {
      vertex.detachDataSources();
      vertex.detachEdges();

      vertexRepository.delete(vertex);
    });

    neo4jVertexService.deleteVertex(vertexId);
  }

  @Override
  public void deleteVertices(@Nonnull List<String> vertexIds) {

    vertexIds.forEach(this::deleteVertex);
  }

  @Override
  public CompletableFuture<Void> batchDeleteVertex(Set<String> types) {

    return CompletableFuture.runAsync(() ->
            deleteVerticesByTypes(types), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步删除顶点任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  @Override
  public CompletableFuture<Void> batchUpdateVertex(String oldType, String newType) {

    return CompletableFuture.runAsync(() ->
            updateVerticesByType(oldType, newType), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步修改顶点任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  private void updateVerticesByType(String oldType, String newType) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Vertex> page;

    do {
      try {
        page = vertexRepository.findAll(
            Specification.where(typeIs(oldType)),
            PageRequest.of(pageNumber, pageSize)
        );
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      final var vertexIds = page.getContent().stream()
          .map(Vertex::getID)
          .collect(Collectors.toList());

      if (CollUtil.isNotEmpty(vertexIds)) {
        try {
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            updateVertices(vertexIds, newType);
            return true;
          });
        } catch (Exception e) {
          log.error("删除顶点ID为 {} 时出现异常", vertexIds, e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
  }

  private void deleteVerticesByTypes(Set<String> types) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Vertex> page;

    do {
      try {
        page = vertexRepository.findAll(
            Specification.where(typesIn(types)),
            PageRequest.of(pageNumber, pageSize)
        );
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      final var vertexIds = page.getContent().stream()
          .map(Vertex::getID)
          .collect(Collectors.toList());

      if (CollUtil.isNotEmpty(vertexIds)) {
        try {
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            deleteVertices(vertexIds);
            return true;
          });
        } catch (Exception e) {
          log.error("删除顶点ID为 {} 时出现异常", vertexIds, e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
  }

  private @Nonnull Specification<Vertex> typeIs(String type) {

    return (root, query, criteriaBuilder) -> {
      if (StrUtil.isBlank(type)) {
        return criteriaBuilder.and();
      }
      return criteriaBuilder.equal(root.get(Vertex_.TYPE), type);
    };
  }

  private @Nonnull Specification<Vertex> addKeyValueSpecifications(
      @Nonnull Map<String, String> keyValues, Specification<Vertex> specification) {

    for (final var entry : keyValues.entrySet()) {
      specification = specification.and(propertyValueIs(entry.getKey(), entry.getValue()));
    }
    return specification;
  }

  private static @Nonnull Specification<Vertex> propertyValueIs(String key, String valueMd5) {

    return (root, query, criteriaBuilder) -> {
      Objects.requireNonNull(query).distinct(true);
      final var propertyJoin = root.<Vertex, Property>join(Vertex_.PROPERTIES, JoinType.LEFT);
      final var propertyValueJoin = propertyJoin
          .<Property, PropertyValue>join(Property_.VALUES, JoinType.LEFT);

      return criteriaBuilder.and(
          criteriaBuilder.equal(propertyJoin.get(Property_.KEY), key),
          criteriaBuilder.equal(propertyValueJoin.get(PropertyValue_.MD5), valueMd5));
    };
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
