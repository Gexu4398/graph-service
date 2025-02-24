package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.Property;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.Vertex;
import java.util.Optional;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends BaseNodeRepository<Property, Long> {

  @Query("""
      MATCH (v:Vertex)-[:HAS_PROPERTY]->(p:Property)
      WHERE v.id = $vertex.id
      AND p.name = $name
      RETURN p LIMIT 1
      """)
  Optional<Property> findByVertexAndName(Vertex vertex, String name);
}
