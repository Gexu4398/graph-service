package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.PropertyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.PropertyValueNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.EdgeNodeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.FeatureNodeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.PropertyNodeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.PropertyValueRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.VertexNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class VertexServiceImpl implements VertexService {

  private final VertexNodeRepository vertexNodeRepository;

  private final EdgeNodeRepository edgeNodeRepository;

  private final PropertyNodeRepository propertyNodeRepository;

  private final PropertyValueRepository propertyValueRepository;

  private final FeatureNodeRepository featureNodeRepository;

  @Autowired
  public VertexServiceImpl(VertexNodeRepository vertexNodeRepository,
      EdgeNodeRepository edgeNodeRepository,
      PropertyNodeRepository propertyNodeRepository,
      PropertyValueRepository propertyValueRepository,
      FeatureNodeRepository featureNodeRepository) {

    this.vertexNodeRepository = vertexNodeRepository;
    this.edgeNodeRepository = edgeNodeRepository;
    this.propertyNodeRepository = propertyNodeRepository;
    this.propertyValueRepository = propertyValueRepository;
    this.featureNodeRepository = featureNodeRepository;
  }

  @Override
  public VertexNode newVertex(@Nonnull NewVertexRequest request) {

    final var vertex = new VertexNode();
    vertex.setName(request.getName());
    vertex.setType(request.getType());

    return vertexNodeRepository.save(vertex);
  }

  @Override
  public VertexNode updateVertex(@Nonnull VertexNode vertexNode,
      @Nonnull NewVertexRequest request) {

    vertexNode.setName(request.getName());
    vertexNode.setType(request.getType());
    return vertexNodeRepository.save(vertexNode);
  }

  @Override
  public VertexNode getVertex(String id) {

    return vertexNodeRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
  }

  @Override
  public Page<VertexNode> getVertices(String keyword, Set<String> types,
      Map<String, String> properties,
      Pageable pageable) {

    final var vertex = Cypher.node("VertexNode").named("vertexNode");
    final var name = vertex.property("name");
    final var type = vertex.property("type");

    var condition = Cypher.noCondition();
    if (StrUtil.isNotBlank(keyword)) {
      condition = name.contains(Cypher.literalOf(keyword));
    }

    if (CollUtil.isNotEmpty(types)) {
      Expression namesList = Cypher.listOf(types.stream()
          .map(Cypher::literalOf)
          .toArray(Expression[]::new));
      final var typeIn = type.in(namesList);
      condition = condition.and(typeIn);
    }

    if (CollUtil.isNotEmpty(properties)) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        condition = condition.and(propertyValueIs(entry.getKey(), entry.getValue(), vertex));
      }
    }

    return vertexNodeRepository.findAll(condition, pageable);
  }

  private static @Nonnull Condition propertyValueIs(@Nonnull String key, @Nonnull String value,
      @Nonnull Node vertex) {

    final var propertyAlias = "prop_" + key;
    final var valueAlias = "val_" + key;

    final var propertyNode = Cypher.node("PropertyNode")
        .named(propertyAlias)
        .withProperties("name", Cypher.literalOf(key));

    final var valueNode = Cypher.node("PropertyValueNode")
        .named(valueAlias)
        .withProperties("value", Cypher.literalOf(value));

    // 构建路径模式
    final var pathPattern = vertex
        .relationshipTo(propertyNode, "HAS_PROPERTY")
        .relationshipTo(valueNode, "HAS_VALUE");

    return Cypher.exists(pathPattern);
  }

  @Override
  public void newVertexProperty(VertexNode vertexNode, String key,
      NewPropertyRequest newPropertyRequest) {

    final var managedProperty = propertyNodeRepository.findByVertexAndName(vertexNode, key)
        .orElseGet(() -> {
          final var property = new PropertyNode();
          property.setName(key);
          return propertyNodeRepository.save(property);
        });

    final var propertyValue = new PropertyValueNode();
    propertyValue.setValue(newPropertyRequest.getValue());
    final var managedPropertyValue = propertyValueRepository.save(propertyValue);
    managedProperty.getValues().add(managedPropertyValue);

    final var property = propertyNodeRepository.save(managedProperty);
    vertexNode.getProperties().add(property);
    vertexNodeRepository.save(vertexNode);
  }
}
