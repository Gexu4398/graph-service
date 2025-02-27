package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.PropertyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import java.util.Optional;

import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyNodeRepository extends BaseNodeRepository<PropertyNode, Long> {

  @Query("""
      MATCH (v:VertexNode)-[:HAS_PROPERTY]->(p:PropertyNode)
      WHERE v.id = $vertexNode.id
      AND p.name = $name
      RETURN p LIMIT 1
      """)
  Optional<PropertyNode> findByVertexAndName(VertexNode vertexNode, String name);
}
