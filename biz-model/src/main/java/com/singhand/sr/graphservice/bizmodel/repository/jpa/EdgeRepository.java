package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface EdgeRepository extends BaseRepository<Edge, Long> {

  boolean existsByNameAndInVertexAndOutVertexAndScope(String name, Vertex inVertex,
      Vertex outVertex, String scope);

  Page<Edge> findByNameAndInVertex_TypeAndOutVertex_Type(String name, String inVertexType,
      String outVertexType, Pageable pageable);

  Page<Edge> findByName(String name, Pageable pageable);

  Optional<Edge> findByNameAndInVertexAndOutVertexAndScope(String name, Vertex inVertex,
      Vertex outVertex, String scope);
}
