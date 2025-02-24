package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.Vertex;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyValueRepository extends BaseNodeRepository<PropertyValue, Long> {

  @Query("""
      MATCH (v:Vertex)-[:HAS_PROPERTY]->(p:Property)-[:HAS_VALUE]->(pv:PropertyValue)
      WHERE v.id = $vertex.id
      AND p.name = $name
      AND pv.md5 = $md5
      RETURN COUNT(pv) > 0
      """)
  boolean existsByProperty_VertexAndProperty_NameAndMd5(Vertex vertex, String name, String md5);
}
