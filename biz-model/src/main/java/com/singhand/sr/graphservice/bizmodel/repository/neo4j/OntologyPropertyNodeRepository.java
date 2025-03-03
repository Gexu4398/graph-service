package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyPropertyNode;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyPropertyNodeRepository extends
    BaseNodeRepository<OntologyPropertyNode, String> {

  @Query("""
      MATCH (n:OntologyNode {id: $ontologyId})
      RETURN EXISTS( (n)-[:HAS_PROPERTY]->(:OntologyPropertyNode {name: $name}) )
      """)
  boolean existsByOntologyIdAndName(String ontologyId, String name);
}
