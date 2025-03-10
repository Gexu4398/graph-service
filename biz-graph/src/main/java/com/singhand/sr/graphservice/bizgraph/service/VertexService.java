package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VertexService {

  Vertex newVertex(NewVertexRequest request);

  Optional<Vertex> getVertex(String id);

  Page<Vertex> getVertices(String keyword, Set<String> types, Map<String, String> keyValues,
      Pageable pageable);

  void deleteVertex(String id);

  void deleteVertices(List<String> vertexIds);

  void batchDeleteVertex(Set<String> types);

  void batchUpdateVertex(String oldName, String newName);
}
