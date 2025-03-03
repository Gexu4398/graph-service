package com.singhand.sr.graphservice.bizmodel.repository.neo4j;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.RelationNode;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationNodeRepository extends BaseNodeRepository<RelationNode, String>{

  boolean existsByName(String name);
}
