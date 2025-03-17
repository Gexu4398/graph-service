package com.singhand.sr.graphservice.bizgraph.helper;

import cn.hutool.core.collection.CollUtil;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jVertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
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
    Long lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Edge> edges;
        if (lastId == null) {
          edges = edgeRepository.findTop500ByNameOrderByID(name);
        } else {
          edges = edgeRepository.findTop500ByNameAndIDGreaterThanOrderByID(name, lastId);
        }

        hasMore = CollUtil.isNotEmpty(edges) && edges.size() == pageSize;

        if (CollUtil.isNotEmpty(edges)) {
          try {
            final var edgeSet = new HashSet<>(edges);
            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              edgeSet.forEach(
                  it -> edgeRepository.findById(it.getID()).ifPresent(this::deleteEdge));
              return true;
            });

            lastId = edges.getLast().getID();
          } catch (Exception e) {
            log.error("删除实体关系出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询边关系时出现异常", e);
        break;
      }
    }
  }

  private void deleteVertexEdge(String name, String inVertexType, String outVertexType) {

    int pageSize = 500;
    Long lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Edge> edges;
        if (lastId == null) {
          edges = edgeRepository.findTop500ByNameAndInVertex_TypeAndOutVertex_TypeOrderByID(
              name, inVertexType, outVertexType);
        } else {
          edges = edgeRepository.findTop500ByNameAndInVertex_TypeAndOutVertex_TypeAndIDGreaterThanOrderByID(
              name, inVertexType, outVertexType, lastId);
        }

        hasMore = CollUtil.isNotEmpty(edges) && edges.size() == pageSize;

        if (CollUtil.isNotEmpty(edges)) {
          try {
            final var edgeSet = new HashSet<>(edges);
            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              edgeSet.forEach(
                  it -> edgeRepository.findById(it.getID()).ifPresent(this::deleteEdge));
              return true;
            });

            lastId = edges.getLast().getID();
          } catch (Exception e) {
            log.error("删除实体关系出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询边关系时出现异常", e);
        break;
      }
    }
  }

  private void updateVertexEdge(String name, String newName, String inVertexType,
      String outVertexType) {

    int pageSize = 500;
    Long lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Edge> edges;
        if (lastId == null) {
          // 第一页查询
          edges = edgeRepository.findTop500ByNameAndInVertex_TypeAndOutVertex_TypeOrderByID(
              name, inVertexType, outVertexType);
        } else {
          // 基于上一页最后ID的查询
          edges = edgeRepository.findTop500ByNameAndInVertex_TypeAndOutVertex_TypeAndIDGreaterThanOrderByID(
              name, inVertexType, outVertexType, lastId);
        }

        hasMore = CollUtil.isNotEmpty(edges) && edges.size() == pageSize;

        if (CollUtil.isNotEmpty(edges)) {
          try {
            final var edgeSet = new HashSet<>(edges);
            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              edgeSet.forEach(it ->
                  edgeRepository.findById(it.getID()).ifPresent(edge -> {
                    edge.setName(newName);
                    edgeRepository.save(edge);

                    neo4jVertexService
                        .updateEdge(name, newName, edge.getInVertex(), edge.getOutVertex());
                  }));
              return true;
            });

            // 记录最后一个处理的ID
            lastId = edges.getLast().getID();
          } catch (Exception e) {
            log.error("修改实体关系出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询边关系时出现异常", e);
        break;
      }
    }
  }

  private void updateVertexEdge(String name, String newName) {

    int pageSize = 500;
    Long lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Edge> edges;
        if (lastId == null) {
          edges = edgeRepository.findTop500ByNameOrderByID(name);
        } else {
          edges = edgeRepository.findTop500ByNameAndIDGreaterThanOrderByID(name, lastId);
        }

        hasMore = CollUtil.isNotEmpty(edges) && edges.size() == pageSize;

        if (CollUtil.isNotEmpty(edges)) {
          try {
            final var edgeSet = new HashSet<>(edges);
            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              edgeSet.forEach(it ->
                  edgeRepository.findById(it.getID()).ifPresent(edge -> {
                    edge.setName(newName);
                    edgeRepository.save(edge);

                    neo4jVertexService
                        .updateEdge(name, newName, edge.getInVertex(), edge.getOutVertex());
                  }));
              return true;
            });
            lastId = edges.getLast().getID();
          } catch (Exception e) {
            log.error("修改实体关系出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询边关系时出现异常", e);
        break;
      }
    }
  }

  private void deleteVertexPropertyKey(Set<String> vertexTypes, String key) {

    int pageSize = 500;
    String lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Vertex> vertices;
        if (lastId == null) {
          vertices = vertexRepository.findTop500ByTypeInOrderByID(vertexTypes);
        } else {
          vertices = vertexRepository.findTop500ByTypeInAndIDGreaterThanOrderByID(vertexTypes,
              lastId);
        }

        hasMore = CollUtil.isNotEmpty(vertices) && vertices.size() == pageSize;

        if (CollUtil.isNotEmpty(vertices)) {
          try {
            final var vertexSet = new HashSet<>(vertices);
            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              vertexSet.forEach(it ->
                  vertexRepository.findById(it.getID())
                      .ifPresent(vertex ->
                          propertyRepository.findByVertexAndKey(vertex, key)
                              .ifPresent(property -> deleteProperty(vertex, property.getKey()))));
              return true;
            });

            lastId = vertices.getLast().getID();
          } catch (Exception e) {
            log.error("删除实体属性key出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询顶点时出现异常", e);
        break;
      }
    }
  }

  private void updateVertexPropertyKey(Set<String> vertexTypes, String oldKey, String newKey) {

    int pageSize = 500;
    String lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Vertex> vertices;
        if (lastId == null) {
          vertices = vertexRepository.findTop500ByTypeInOrderByID(vertexTypes);
        } else {
          vertices = vertexRepository.findTop500ByTypeInAndIDGreaterThanOrderByID(vertexTypes,
              lastId);
        }

        hasMore = CollUtil.isNotEmpty(vertices) && vertices.size() == pageSize;

        if (CollUtil.isNotEmpty(vertices)) {
          try {
            final var vertexSet = new HashSet<>(vertices);
            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              vertexSet.forEach(it ->
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

            lastId = vertices.getLast().getID();
          } catch (Exception e) {
            log.error("修改实体属性key出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询顶点时出现异常", e);
        break;
      }
    }
  }

  private void updateVerticesByType(String oldType, String newType) {

    int pageSize = 500;
    String lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Vertex> vertices;
        if (lastId == null) {
          vertices = vertexRepository.findTop500ByTypeOrderByID(oldType);
        } else {
          vertices = vertexRepository.findTop500ByTypeAndIDGreaterThanOrderByID(oldType, lastId);
        }

        hasMore = CollUtil.isNotEmpty(vertices) && vertices.size() == pageSize;

        if (CollUtil.isNotEmpty(vertices)) {
          try {
            final var vertexIds = vertices.stream()
                .map(Vertex::getID)
                .collect(Collectors.toList());

            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              updateVertices(vertexIds, newType);
              return true;
            });

            lastId = vertices.getLast().getID();
          } catch (Exception e) {
            log.error("更新顶点类型时出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询顶点时出现异常", e);
        break;
      }
    }
  }

  private void deleteVerticesByTypes(Set<String> types) {

    int pageSize = 500;
    String lastId = null;
    boolean hasMore = true;

    while (hasMore) {
      try {
        List<Vertex> vertices;
        if (lastId == null) {
          vertices = vertexRepository.findTop500ByTypeInOrderByID(types);
        } else {
          vertices = vertexRepository.findTop500ByTypeInAndIDGreaterThanOrderByID(types, lastId);
        }

        hasMore = CollUtil.isNotEmpty(vertices) && vertices.size() == pageSize;

        if (CollUtil.isNotEmpty(vertices)) {
          try {
            final var vertexIds = vertices.stream()
                .map(Vertex::getID)
                .collect(Collectors.toList());

            final var transaction = new TransactionTemplate(bizTransactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
              deleteVertices(vertexIds);
              return true;
            });

            lastId = vertices.getLast().getID();
          } catch (Exception e) {
            log.error("删除顶点时出现异常", e);
          }
        }
      } catch (Exception e) {
        log.error("查询顶点时出现异常", e);
        break;
      }
    }
  }
}
