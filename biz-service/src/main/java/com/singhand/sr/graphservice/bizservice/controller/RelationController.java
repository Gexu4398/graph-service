package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizgraph.service.RelationService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.RelationNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.RelationNodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("relation")
@Tag(name = "关系模型管理")
@Validated
public class RelationController {

  private final RelationService relationService;

  private final RelationNodeRepository relationNodeRepository;

  @Autowired
  public RelationController(RelationService relationService,
      RelationNodeRepository relationNodeRepository) {

    this.relationService = relationService;
    this.relationNodeRepository = relationNodeRepository;
  }

  @Operation(summary = "获取关系模型详情")
  @GetMapping("{id}")
  @SneakyThrows
  public RelationNode getRelation(@PathVariable String id) {

    return relationService.getRelation(id);
  }

  @Operation(summary = "查询关系模型")
  @GetMapping
  @SneakyThrows
  public Page<RelationNode> getRelations(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
      Pageable pageable) {

    return relationService.getRelations(keyword, pageable);
  }

  @Operation(summary = "添加关系模型")
  @PostMapping
  @SneakyThrows
  @Transactional("bizNeo4jTransactionManager")
  public RelationNode newRelation(@RequestParam String name) {

    if (relationNodeRepository.existsByName(name)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系模型已存在");
    }

    return relationService.newRelation(name);
  }

  @Operation(summary = "修改关系模型")
  @PutMapping("{id}")
  @SneakyThrows
  @Transactional("bizNeo4jTransactionManager")
  public RelationNode updateRelation(@PathVariable String id, @RequestParam String newName) {

    final var relation = relationService.getRelation(id);

    if (relation.getName().equals(newName)) {
      return relation;
    }

    if (relationNodeRepository.existsByName(newName)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "关系模型已存在");
    }

    return relationService.updateRelation(relation, newName);
  }

  @Operation(summary = "删除关系模型")
  @DeleteMapping("{id}")
  @SneakyThrows
  public void deleteRelation(@PathVariable String id) {

    final var relation = relationService.getRelation(id);

    relationService.deleteRelation(relation);
  }
}
