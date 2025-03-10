package com.singhand.sr.graphservice.bizgraph.service.impl.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.VertexNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
