package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyNodeRepository extends BaseNodeRepository<OntologyNode, String> {

  boolean existsByName(@NotBlank String name);

  @Query("""
      MATCH (o:OntologyNode)-[:HAS_RELATION]->(relation:OntologyRelationNode {name: $relationName})
      RETURN o
      """)
  Set<OntologyNode> findByRelationName(String relationName);
}
