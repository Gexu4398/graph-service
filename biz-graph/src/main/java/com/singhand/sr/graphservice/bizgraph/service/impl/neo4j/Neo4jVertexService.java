package com.singhand.sr.graphservice.bizgraph.service.impl.neo4j;

import cn.hutool.core.collection.CollUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.EdgeRelation;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.VertexNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@Transactional(transactionManager = "bizNeo4jTransactionManager")
public class Neo4jVertexService {

  private final VertexNodeRepository vertexNodeRepository;

  private final Neo4jVectorStore vectorStore;

  @Autowired
  public Neo4jVertexService(VertexNodeRepository vertexNodeRepository,
      Neo4jVectorStore vectorStore) {

    this.vertexNodeRepository = vertexNodeRepository;
    this.vectorStore = vectorStore;
  }

  public Optional<VertexNode> getVertex(String vertexId) {

    return vertexNodeRepository.findById(vertexId);
  }

  public VertexNode newVertex(@Nonnull Vertex vertex) {

    final var vertexNode = new VertexNode();
    vertexNode.setId(vertex.getID());
    vertexNode.setName(vertex.getName());
    vertexNode.setType(vertex.getType());

    final var managedvertexNode = vertexNodeRepository.save(vertexNode);

    addVertexToVectorStore(managedvertexNode);

    return managedvertexNode;
  }

  public void addVertexToVectorStore(@Nonnull VertexNode vertexNode) {

    StringBuilder relationsInfo = new StringBuilder();
    for (EdgeRelation edge : vertexNode.getEdges()) {
      relationsInfo.append("关系类型: ").append(edge.getName())
          .append(", 目标节点: ").append(edge.getVertexNode().getName())
          .append("; ");
    }

    final var document = new Document(
        vertexNode.getId(),
        vertexNode.getName(),
        Map.of(
            "name", vertexNode.getName(),
            "type", vertexNode.getType(),
            "properties", vertexNode.getProperties().toString(),
            "relations", relationsInfo.toString()
        )
    );
    try {
      vectorStore.delete(List.of(vertexNode.getId()));
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    vectorStore.add(List.of(document));
  }

  public void updateVectorStore(@Nonnull String id) {

    final var vertexNode = getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    addVertexToVectorStore(vertexNode);
  }

  public void updateVectorStore(@Nonnull VertexNode vertexNode) {

    addVertexToVectorStore(vertexNode);
  }

  public void deleteVertex(String vertexId) {

    getVertex(vertexId).ifPresent(it -> {
      vertexNodeRepository.delete(it);
      vectorStore.delete(List.of(it.getId()));
    });
  }

  public void updateVertices(List<String> vertexIds, String newType) {

    final var vertexNodes = vertexNodeRepository.findAllById(vertexIds);

    final var managedVertexNodes = new HashSet<>(vertexNodes);

    managedVertexNodes.forEach(it -> {
      it.setType(newType);
      final var managedVertexNode = vertexNodeRepository.save(it);
      updateVectorStore(managedVertexNode);
    });
  }

  public void updateVertex(String id, String name) {

    getVertex(id).ifPresent(it -> {
      it.setName(name);
      final var managedVertexNode = vertexNodeRepository.save(it);
      updateVectorStore(managedVertexNode);
    });
  }

  public void newProperty(@Nonnull Vertex vertex, @Nonnull NewPropertyRequest request) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var values = vertexNode.getProperties()
        .computeIfAbsent(request.getKey(), v -> new HashSet<>());

    values.add(request.getValue());

    final var managedVertexNode = vertexNodeRepository.save(vertexNode);

    updateVectorStore(managedVertexNode);
  }

  public void updateProperty(@Nonnull Vertex vertex, @Nonnull UpdatePropertyRequest request) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexNode.getProperties().get(request.getKey()).remove(request.getOldValue());
    vertexNode.getProperties().get(request.getKey()).add(request.getNewValue());

    final var managedVertexNode = vertexNodeRepository.save(vertexNode);

    updateVectorStore(managedVertexNode);
  }

  public void deleteProperty(@Nonnull Vertex vertex, @Nonnull String key, @Nonnull String value) {

    final var vertexNode = getVertex(vertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexNode.getProperties().get(key).remove(value);

    if (CollUtil.isEmpty(vertexNode.getProperties().get(key))) {
      vertexNode.getProperties().remove(key);
    }

    final var managedVertexNode = vertexNodeRepository.save(vertexNode);
    updateVectorStore(managedVertexNode);
  }

  public void newEdge(@Nonnull String name, @Nonnull Vertex inVertex, @Nonnull Vertex outVertex) {

    final var inVertexNode = getVertex(inVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertexNode = getVertex(outVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var edgeRelation = new EdgeRelation();
    edgeRelation.setName(name);
    edgeRelation.setVertexNode(outVertexNode);

    inVertexNode.getEdges().add(edgeRelation);

    vertexNodeRepository.save(inVertexNode);
  }

  public void deleteEdge(@Nonnull String name, @Nonnull Vertex inVertex,
      @Nonnull Vertex outVertex) {

    final var inVertexNode = getVertex(inVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertexNode = getVertex(outVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var edge = inVertexNode.getEdges().stream()
        .filter(it -> it.getName().equals(name) &&
            it.getVertexNode().equals(outVertexNode))
        .findFirst()
        .orElse(null);

    if (null == edge) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
    }

    inVertexNode.getEdges().remove(edge);

    vertexNodeRepository.deleteRelation(inVertex.getID(), outVertex.getID(), name);

    vertexNodeRepository.save(inVertexNode);
  }

  public void updateEdge(@Nonnull String oldName, @Nonnull String newName, @Nonnull Vertex inVertex,
      @Nonnull Vertex outVertex) {

    final var inVertexNode = getVertex(inVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertexNode = getVertex(outVertex.getID())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    inVertexNode.getEdges().stream()
        .filter(it -> it.getName().equals(oldName) &&
            it.getVertexNode().equals(outVertexNode))
        .findFirst()
        .ifPresentOrElse(it -> {
          it.setName(newName);
          vertexNodeRepository.save(inVertexNode);
        }, () -> {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
        });
  }
}
