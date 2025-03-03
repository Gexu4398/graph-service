package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyPropertyNode;
import org.springframework.stereotype.Repository;

@Repository
public interface OntologyPropertyNodeRepository extends
    BaseNodeRepository<OntologyPropertyNode, String> {

}
