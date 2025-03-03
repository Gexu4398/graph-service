package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyNodeRepository extends BaseNodeRepository<OntologyNode, String> {

  boolean existsByName(@NotBlank String name);

  @Query("""
      MATCH (n:OntologyNode {id: $id})
      RETURN EXISTS( (n)-[:HAS_PROPERTY]->(:OntologyPropertyNode {name: $propertyName}) )
      """)
  boolean existsByIdAndPropertyName(String id, String propertyName);
}
