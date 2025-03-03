package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyNodeRepository extends BaseNodeRepository<OntologyNode, String> {

  boolean existsByName(@NotBlank String name);
}
