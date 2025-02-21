package com.singhand.sr.graphservice.bizgraph.service.impl;

import com.singhand.sr.graphservice.bizgraph.model.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.*;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class VertexServiceImpl implements VertexService {

  private final VertexRepository vertexRepository;

  private final EdgeRepository edgeRepository;

  private final PropertyRepository propertyRepository;

  private final PropertyValueRepository propertyValueRepository;

  private final FeatureRepository featureRepository;

  private final Neo4jTemplate neo4jTemplate;

  @Autowired
  public VertexServiceImpl(VertexRepository vertexRepository, EdgeRepository edgeRepository,
      PropertyRepository propertyRepository, PropertyValueRepository propertyValueRepository,
      FeatureRepository featureRepository, Neo4jTemplate neo4jTemplate) {

    this.vertexRepository = vertexRepository;
    this.edgeRepository = edgeRepository;
    this.propertyRepository = propertyRepository;
    this.propertyValueRepository = propertyValueRepository;
    this.featureRepository = featureRepository;
    this.neo4jTemplate = neo4jTemplate;
  }

  @Override
  public Vertex newVertex(@Nonnull NewVertexRequest request) {

    final var vertex = new Vertex();
    vertex.setName(request.getName());
    vertex.setType(request.getType());

    return vertexRepository.save(vertex);
  }

  @Override
  public Vertex updateVertex(@Nonnull Vertex vertex, @Nonnull NewVertexRequest request) {

    vertex.setName(request.getName());
    vertex.setType(request.getType());
    return vertexRepository.save(vertex);
  }

  @Override
  public Vertex getVertex(String id) {

    return vertexRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
  }

  @Override
  public Page<Vertex> getVertices(String keyword, Set<String> types, Map<String, String> properties,
      Pageable pageable) {

    final var vertex = Cypher.node("Vertex").named("vertex");
    final var name = vertex.property("name");
    final var type = vertex.property("type");

    return null;
  }
}
