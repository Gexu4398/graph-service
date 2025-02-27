package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.PropertyValueNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyValueRepository extends BaseNodeRepository<PropertyValueNode, Long> {

  @Query("""
      MATCH (v:VertexNode)-[:HAS_PROPERTY]->(p:PropertyNode)-[:HAS_VALUE]->(pv:PropertyValueNode)
      WHERE v.id = $vertexNode.id
      AND p.name = $name
      AND pv.md5 = $md5
      RETURN COUNT(pv) > 0
      """)
  boolean existsByProperty_VertexNodeAndPropertyNode_NameAndMd5(VertexNode vertexNode, String name, String md5);
}
