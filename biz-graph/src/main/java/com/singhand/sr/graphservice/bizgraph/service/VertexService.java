package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VertexService {

  Vertex newVertex(@Valid NewVertexRequest request);

  Optional<Vertex> getVertex(String id);

  Page<Vertex> getVertices(String keyword, Set<String> types, Map<String, String> keyValues,
      Pageable pageable);
}
