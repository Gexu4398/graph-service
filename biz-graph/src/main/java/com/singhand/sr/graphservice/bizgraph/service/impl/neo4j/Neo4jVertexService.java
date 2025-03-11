package com.singhand.sr.graphservice.bizgraph.service.impl.neo4j;

import cn.hutool.core.collection.CollUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.VertexNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class Neo4jVertexService {

  private final VertexNodeRepository vertexNodeRepository;

  @Autowired
  public Neo4jVertexService(VertexNodeRepository vertexNodeRepository) {

    this.vertexNodeRepository = vertexNodeRepository;
  }

  public Optional<VertexNode> getVertex(String vertexId) {

    return vertexNodeRepository.findById(vertexId);
  }

  public VertexNode newVertex(@Nonnull Vertex vertex) {

    final var vertexNode = new VertexNode();
    vertexNode.setId(vertex.getID());
    vertexNode.setName(vertex.getName());
    vertexNode.setType(vertex.getType());

    return vertexNodeRepository.save(vertexNode);
  }

  public void deleteVertex(String vertexId) {

    getVertex(vertexId).ifPresent(vertexNodeRepository::delete);
  }

  public void updateVertices(List<String> vertexIds, String newType) {

    final var vertexNodes = vertexNodeRepository.findAllById(vertexIds);

    final var managedVertexNode = new HashSet<>(vertexNodes);

    managedVertexNode.forEach(it -> {
      it.setType(newType);
      vertexNodeRepository.save(it);
    });
  }

  public void updateVertex(String id, String name) {

    getVertex(id).ifPresent(it -> {
      it.setName(name);
      vertexNodeRepository.save(it);
    });
  }

  public void newProperty(@Nonnull Vertex vertex, @Nonnull NewPropertyRequest request) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var values = vertexNode.getProperties()
        .computeIfAbsent(request.getKey(), v -> new HashSet<>());

    values.add(request.getValue());

    vertexNodeRepository.save(vertexNode);
  }

  public void updateProperty(@Nonnull Vertex vertex, @Nonnull UpdatePropertyRequest request) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexNode.getProperties().get(request.getKey()).remove(request.getOldValue());
    vertexNode.getProperties().get(request.getKey()).add(request.getNewValue());

    vertexNodeRepository.save(vertexNode);
  }

  public void deleteProperty(@Nonnull Vertex vertex, @Nonnull String key, @Nonnull String value) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexNode.getProperties().get(key).remove(value);

    if (CollUtil.isEmpty(vertexNode.getProperties().get(key))) {
      vertexNode.getProperties().remove(key);
    }

    vertexNodeRepository.save(vertexNode);
  }
}
