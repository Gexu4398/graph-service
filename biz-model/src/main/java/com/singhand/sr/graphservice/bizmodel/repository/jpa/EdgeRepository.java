package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface EdgeRepository extends BaseRepository<Edge, Long> {

  boolean existsByNameAndInVertexAndOutVertexAndScope(String name, Vertex inVertex,
      Vertex outVertex, String scope);

  Optional<Edge> findByNameAndInVertexAndOutVertexAndScope(String name, Vertex inVertex,
      Vertex outVertex, String scope);
}
