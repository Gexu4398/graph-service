package com.singhand.sr.graphservice.bizgraph.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jVertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex_;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EdgeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class VertexServiceHelper {

  private final VertexRepository vertexRepository;

  private final EdgeRepository edgeRepository;

  private final PropertyRepository propertyRepository;

  private final PlatformTransactionManager bizTransactionManager;

  private final Neo4jVertexService neo4jVertexService;

  private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  private final OntologyService ontologyService;

  @Autowired
  public VertexServiceHelper(VertexRepository vertexRepository, EdgeRepository edgeRepository,
      PropertyRepository propertyRepository,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager,
      Neo4jVertexService neo4jVertexService, OntologyService ontologyService) {

    this.vertexRepository = vertexRepository;
    this.edgeRepository = edgeRepository;
    this.propertyRepository = propertyRepository;
    this.bizTransactionManager = bizTransactionManager;
    this.neo4jVertexService = neo4jVertexService;
    this.ontologyService = ontologyService;
  }

  public void deleteEdge(@Nonnull Edge edge) {

    final var inVertexId = edge.getInVertex().getID();
    final var outVertexId = edge.getOutVertex().getID();

    edge.clearEvidences();
    edge.clearProperties();
    edge.detachVertices();
    edgeRepository.delete(edge);

    neo4jVertexService.deleteEdge(edge.getName(), inVertexId, outVertexId);
  }

  public void deleteVertex(String vertexId) {

    vertexRepository.findById(vertexId).ifPresent(vertex -> {
      vertex.detachDataSources();
      vertex.detachEdges();
      vertexRepository.delete(vertex);
    });

    neo4jVertexService.deleteVertex(vertexId);
  }

  public void deleteVertices(@Nonnull List<String> vertexIds) {

    vertexIds.forEach(this::deleteVertex);
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

  public void deleteProperty(@Nonnull Vertex vertex, @Nonnull String key) {

    final var dbProperty = Optional.ofNullable(vertex.getProperties().get(key))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在"));

    dbProperty.clearValues();
    propertyRepository.save(dbProperty);

    vertex.getProperties().remove(key);
    dbProperty.setVertex(null);
    vertexRepository.save(vertex);

    neo4jVertexService.deleteProperty(vertex, key);
  }

  public void batchDeleteVertex(Set<String> types) {

    CompletableFuture.runAsync(() ->
            deleteVerticesByTypes(types), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步删除顶点任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  public void batchUpdateVertex(String oldType, String newType) {

    CompletableFuture.runAsync(() ->
            updateVerticesByType(oldType, newType), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步修改顶点任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  public void batchUpdateVertexProperty(String vertexType, String oldKey, String newKey) {

    CompletableFuture.runAsync(() -> {
          // 需要考虑到子集本体的属性
          final var types = ontologyService.getAllSubOntologies(Set.of(vertexType));
          updateVertexPropertyKey(types, oldKey, newKey);
        }, VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步修改顶点属性任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  public void batchDeleteVertexProperty(String vertexType, String key) {

    CompletableFuture.runAsync(() -> {
          // 需要考虑到子集本体的属性
          final var types = ontologyService.getAllSubOntologies(Set.of(vertexType));
          deleteVertexPropertyKey(types, key);
        }, VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步删除顶点属性任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  public void batchUpdateVertexEdge(String name, String newName, String inVertexType,
      String outVertexType) {

    CompletableFuture.runAsync(() ->
            updateVertexEdge(name, newName, inVertexType, outVertexType), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步修改顶点关系任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  public void batchUpdateVertexEdge(String name, String newName) {

    CompletableFuture.runAsync(() ->
            updateVertexEdge(name, newName), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步修改顶点关系任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  public void batchDeleteVertexEdge(String name, String inVertexType, String outVertexType) {

    CompletableFuture.runAsync(() ->
            deleteVertexEdge(name, inVertexType, outVertexType), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步删除顶点关系任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  public void batchDeleteVertexEdge(String name) {

    CompletableFuture.runAsync(() ->
            deleteVertexEdge(name), VIRTUAL_EXECUTOR)
        .exceptionally(ex -> {
          log.error("异步删除顶点关系任务出现异常", ex);
          throw ex instanceof CompletionException ?
              (CompletionException) ex : new CompletionException(ex);
        });
  }

  private void deleteVertexEdge(String name) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Edge> page;

    do {
      try {
        final var pageRequest = PageRequest.of(pageNumber, pageSize);
        page = edgeRepository.findByName(name, pageRequest);
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      if (CollUtil.isNotEmpty(page.getContent())) {
        try {
          final var edges = new HashSet<>(page.getContent());
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            edges.forEach(it -> edgeRepository.findById(it.getID()).ifPresent(this::deleteEdge));
            return true;
          });
        } catch (Exception e) {
          log.error("删除实体关系出现异常", e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
  }

  private void deleteVertexEdge(String name, String inVertexType, String outVertexType) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Edge> page;

    do {
      try {
        final var pageRequest = PageRequest.of(pageNumber, pageSize);
        page = edgeRepository.findByNameAndInVertex_TypeAndOutVertex_Type(name, inVertexType,
            outVertexType, pageRequest);
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      if (CollUtil.isNotEmpty(page.getContent())) {
        try {
          final var edges = new HashSet<>(page.getContent());
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            edges.forEach(it -> edgeRepository.findById(it.getID()).ifPresent(this::deleteEdge));
            return true;
          });
        } catch (Exception e) {
          log.error("删除实体关系出现异常", e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
  }

  private void updateVertexEdge(String name, String newName, String inVertexType,
      String outVertexType) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Edge> page;

    do {
      try {
        final var pageRequest = PageRequest.of(pageNumber, pageSize);
        page = edgeRepository.findByNameAndInVertex_TypeAndOutVertex_Type(name, inVertexType,
            outVertexType, pageRequest);
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      if (CollUtil.isNotEmpty(page.getContent())) {
        try {
          final var edges = new HashSet<>(page.getContent());
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            edges.forEach(it ->
                edgeRepository.findById(it.getID()).ifPresent(edge -> {
                  edge.setName(newName);
                  edgeRepository.save(edge);

                  neo4jVertexService
                      .updateEdge(name, newName, edge.getInVertex(), edge.getOutVertex());
                }));
            return true;
          });
        } catch (Exception e) {
          log.error("修改实体关系出现异常", e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
  }

  private void updateVertexEdge(String name, String newName) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Edge> page;

    do {
      try {
        final var pageRequest = PageRequest.of(pageNumber, pageSize);
        page = edgeRepository.findByName(name, pageRequest);
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      if (CollUtil.isNotEmpty(page.getContent())) {
        try {
          final var edges = new HashSet<>(page.getContent());
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            edges.forEach(it ->
                edgeRepository.findById(it.getID()).ifPresent(edge -> {
                  edge.setName(newName);
                  edgeRepository.save(edge);

                  neo4jVertexService
                      .updateEdge(name, newName, edge.getInVertex(), edge.getOutVertex());
                }));
            return true;
          });
        } catch (Exception e) {
          log.error("修改实体关系出现异常", e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
  }

  private void deleteVertexPropertyKey(Set<String> vertexTypes, String key) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Vertex> page;

    do {
      try {
        page = vertexRepository.findAll(Specification.where(typesIn(vertexTypes)),
            PageRequest.of(pageNumber, pageSize)
        );
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      if (CollUtil.isNotEmpty(page.getContent())) {
        try {
          final var vertices = new HashSet<>(page.getContent());
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            vertices.forEach(it ->
                vertexRepository.findById(it.getID())
                    .ifPresent(vertex ->
                        propertyRepository.findByVertexAndKey(vertex, key)
                            .ifPresent(property -> deleteProperty(vertex, property.getKey()))));
            return true;
          });
        } catch (Exception e) {
          log.error("删除实体属性key出现异常", e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
  }

  private void updateVertexPropertyKey(Set<String> vertexTypes, String oldKey, String newKey) {

    int pageSize = 500;
    int pageNumber = 0;
    Page<Vertex> page;

    do {
      try {
        page = vertexRepository.findAll(Specification.where(typesIn(vertexTypes)),
            PageRequest.of(pageNumber, pageSize)
        );
      } catch (Exception e) {
        log.error("分页查询第 {} 页时出现异常", pageNumber, e);
        break;
      }

      if (CollUtil.isNotEmpty(page.getContent())) {
        try {
          final var vertices = new HashSet<>(page.getContent());
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transaction.execute(status -> {
            vertices.forEach(it ->
                vertexRepository.findById(it.getID())
                    .ifPresent(vertex ->
                        propertyRepository.findByVertexAndKey(vertex, oldKey)
                            .ifPresent(property -> {
                              property.setKey(newKey);
                              vertex.getProperties().remove(oldKey);
                              vertex.getProperties().put(newKey, property);
                              vertexRepository.save(vertex);
                            })));
            return true;
          });
        } catch (Exception e) {
          log.error("修改实体属性key出现异常", e);
        }
      }

      pageNumber++;
    } while (page.hasNext());
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

  private static @Nonnull Specification<Vertex> typesIn(Set<String> types) {

    return (root, query, criteriaBuilder) -> {
      if (CollUtil.isEmpty(types)) {
        return criteriaBuilder.and();
      }
      return root.get(Vertex_.TYPE).in(types);
    };
  }
}
