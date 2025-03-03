package com.singhand.sr.graphservice.bizgraph.service;

import com.singhand.sr.graphservice.bizmodel.model.neo4j.RelationNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RelationService {

  RelationNode newRelation(String name);

  RelationNode getRelation(String id);

  Page<RelationNode> getRelations(String keyword, Pageable pageable);

  RelationNode updateRelation(RelationNode relation, String newName);

  void deleteRelation(RelationNode relation);
}
