package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public interface EdgeRepository extends BaseRepository<Edge, Long> {

  boolean existsByNameAndInVertexAndOutVertexAndScope(String name, Vertex inVertex,
      Vertex outVertex, String scope);

  Optional<Edge> findByNameAndInVertexAndOutVertexAndScope(String name, Vertex inVertex,
      Vertex outVertex, String scope);

  Set<Edge> findByInVertex_IDAndOutVertex_TypeIn(String inVertexId, Collection<String> type);

  List<Edge> findTop500ByNameAndInVertex_TypeAndOutVertex_TypeOrderByID(String name,
      String inVertexType, String outVertexType);

  List<Edge> findTop500ByNameAndInVertex_TypeAndOutVertex_TypeAndIDGreaterThanOrderByID(String name,
      String inVertexType, String outVertexType, Long lastId);

  List<Edge> findTop500ByNameAndIDGreaterThanOrderByID(String name, Long lastId);

  List<Edge> findTop500ByNameOrderByID(String name);
}
