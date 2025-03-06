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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JpaVertexService implements VertexService {

  private final VertexRepository vertexRepository;

  private final Neo4jVertexService neo4jVertexService;

  @Autowired
  public JpaVertexService(VertexRepository vertexRepository,
      Neo4jVertexService neo4jVertexService) {

    this.vertexRepository = vertexRepository;
    this.neo4jVertexService = neo4jVertexService;
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
