package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.service.RelationService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.RelationNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.RelationNodeRepository;
import jakarta.annotation.Nonnull;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RelationServiceImpl implements RelationService {

  private final RelationNodeRepository relationNodeRepository;

  public RelationServiceImpl(RelationNodeRepository relationNodeRepository) {

    this.relationNodeRepository = relationNodeRepository;
  }

  @Override
  public RelationNode getRelation(String id) {

    return relationNodeRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));
  }

  @Override
  public Page<RelationNode> getRelations(String keyword, Pageable pageable) {

    final var relationNode = Cypher.node("RelationNode").named("relationNode");
    final var name = relationNode.property("name");
    var condition = Cypher.noCondition();

    if (StrUtil.isNotBlank(keyword)) {
      condition = name.contains(Cypher.literalOf(keyword));
    }

    return relationNodeRepository.findAll(condition, pageable);
  }

  @Override
  public RelationNode updateRelation(@Nonnull RelationNode relation, @Nonnull String newName) {

    relation.setName(newName);

    return relationNodeRepository.save(relation);
  }

  @Override
  public void deleteRelation(RelationNode relation) {

    relationNodeRepository.delete(relation);
  }

  @Override
  public RelationNode newRelation(String name) {

    final var relationNode = new RelationNode();
    relationNode.setName(name);

    return relationNodeRepository.save(relationNode);
  }
}
