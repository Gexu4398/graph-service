package com.singhand.sr.graphservice.bizgraph.service.impl.jpa;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizgraph.service.impl.neo4j.Neo4jVertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Property_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex_;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class JpaVertexService implements VertexService {

  private final VertexRepository vertexRepository;

  private final Neo4jVertexService neo4jVertexService;

  private final PlatformTransactionManager bizTransactionManager;

  @Autowired
  public JpaVertexService(VertexRepository vertexRepository,
      Neo4jVertexService neo4jVertexService,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager) {

    this.vertexRepository = vertexRepository;
    this.neo4jVertexService = neo4jVertexService;
    this.bizTransactionManager = bizTransactionManager;
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

    final var vertex = new Vertex();
    vertex.setName(request.getName());
    vertex.setType(request.getType());

    final var managedvertex = vertexRepository.save(vertex);
    neo4jVertexService.newVertex(managedvertex);
    return managedvertex;
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
  public void batchDeleteVertex(Set<String> types) {

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(() -> {
        try {
          deleteVerticesByTypes(types);
        } catch (Exception e) {
          log.error("异步删除顶点任务出现异常", e);
        }
      });
    }
  }

  @Override
  public void batchUpdateVertex(String oldName, String newName) {

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
