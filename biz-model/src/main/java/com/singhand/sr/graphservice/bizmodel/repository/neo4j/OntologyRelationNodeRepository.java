package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyRelationNode;
import java.util.Optional;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyRelationNodeRepository extends BaseNodeRepository<OntologyRelationNode, String> {

  @Query("""
      MATCH (source:OntologyNode {id: $inId})
      -[:HAS_RELATION]->(relation:OntologyRelationNode {name: $name})
      -[:RELATES_TO]->(target:OntologyNode {id: $outId})
      RETURN COUNT(relation) > 0
      """)
  boolean existsRelationBetweenNodes(String inId, String outId, String name);

  @Query("""
      MATCH (source:OntologyNode {id: $inId})
      -[:HAS_RELATION]->(relation:OntologyRelationNode {name: $name})
      -[:RELATES_TO]->(target:OntologyNode {id: $outId})
      RETURN relation
      """)
  Optional<OntologyRelationNode> findRelationBetweenNodes(String inId, String outId, String name);
}
