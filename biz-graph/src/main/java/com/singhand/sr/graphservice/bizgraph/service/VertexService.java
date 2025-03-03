package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VertexService {

  VertexNode newVertex(NewVertexRequest request);

  VertexNode updateVertex(VertexNode vertexNode, NewVertexRequest request);

  VertexNode getVertex(@NotBlank String id);

  Page<VertexNode> getVertices(String keyword, Set<String> types,
      Map<String, String> properties, Pageable pageable);

  void newVertexProperty(VertexNode vertexNode, String key, NewPropertyRequest newPropertyRequest);
}
