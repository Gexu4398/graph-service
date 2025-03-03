package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.RelationService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.RelationNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.RelationNodeRepository;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class RelationServiceImpl implements RelationService {

  private final RelationNodeRepository relationNodeRepository;

  private final OntologyNodeRepository ontologyNodeRepository;

  private final OntologyService ontologyService;

  public RelationServiceImpl(RelationNodeRepository relationNodeRepository,
      OntologyNodeRepository ontologyNodeRepository, OntologyService ontologyService) {

    this.relationNodeRepository = relationNodeRepository;
    this.ontologyNodeRepository = ontologyNodeRepository;
    this.ontologyService = ontologyService;
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
  public RelationNode newRelation(String name) {

    final var relationNode = new RelationNode();
    relationNode.setName(name);

    return relationNodeRepository.save(relationNode);
  }

  @Override
  public RelationNode updateRelation(@Nonnull RelationNode relation, @Nonnull String oldName,
      @Nonnull String newName) {

    relation.setName(newName);

    final var managedRelation = relationNodeRepository.save(relation);

    updateOntologyRelation(managedRelation, oldName);

    return managedRelation;
  }

  @Override
  public void deleteRelation(RelationNode relation) {

    deleteOntologyRelation(relation);

    relationNodeRepository.delete(relation);
  }

  private void updateOntologyRelation(@Nonnull RelationNode relation, String oldName) {

    final var ontologyNodes = new HashSet<>(
        ontologyNodeRepository.findByRelationName(oldName)
    );

    if (CollUtil.isNotEmpty(ontologyNodes)) {
      try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        executor.submit(() -> {
          try {
            ontologyNodes.forEach(it ->
                it.getRelations()
                    .stream()
                    .filter(rn -> rn.getName().equals(relation.getName()))
                    .forEach(rn -> {
                      final var outOntology = rn.getTargetOntologyNode();
                      log.info("正在修改本体关系...in={} name={} out={}", it.getName(),
                          rn.getName(),
                          outOntology.getName()
                      );
                      ontologyService.updateRelation(it, oldName, outOntology, relation.getName());
                    }));
          } catch (Exception e) {
            log.error("修改本体关系失败...message={}", e.getMessage());
            throw new RuntimeException(e);
          }
        });
      }
    }
  }

  private void deleteOntologyRelation(@Nonnull RelationNode relation) {

    final var ontologyNodes = new HashSet<>(
        ontologyNodeRepository.findByRelationName(relation.getName())
    );

    if (CollUtil.isNotEmpty(ontologyNodes)) {
      try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        executor.submit(() -> {
          try {
            ontologyNodes.forEach(it ->
                it.getRelations()
                    .stream()
                    .filter(rn -> rn.getName().equals(relation.getName()))
                    .forEach(rn -> {
                      final var outOntology = rn.getTargetOntologyNode();
                      log.info("正在删除本体关系...in={} name={} out={}", it.getName(),
                          rn.getName(),
                          outOntology.getName()
                      );
                      ontologyService.deleteRelation(it, rn.getName(), outOntology);
                    }));
          } catch (Exception e) {
            log.error("删除本体关系失败...message={}", e.getMessage());
            throw new RuntimeException(e);
          }
        });
      }
    }
  }
}
