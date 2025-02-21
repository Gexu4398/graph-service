package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizgraph.model.NewVertexRequest;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.Vertex;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VertexService {

  Vertex newVertex(NewVertexRequest request);

  Vertex updateVertex(Vertex vertex, NewVertexRequest request);

  Vertex getVertex(@NotBlank String id);

  Page<Vertex> getVertices(String keyword, Set<String> types,
      Map<String, String> properties, Pageable pageable);
}
