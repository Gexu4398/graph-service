package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizgraph.service.RelationModelService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("relationModel")
@Tag(name = "关系模型管理")
@Validated
public class RelationModelController {

  private final RelationModelService relationModelService;

  @Autowired
  public RelationModelController(RelationModelService relationModelService) {

    this.relationModelService = relationModelService;
  }

  @Operation(summary = "获取关系模型详情")
  @GetMapping(path = "{id}")
  public RelationModel getRelationModel(@PathVariable Long id) {

    return relationModelService.getRelationModel(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系模型不存在"));
  }

  @Operation(summary = "查询关系模型")
  @GetMapping
  public List<RelationModel> getRelationModels(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword) {

    return relationModelService.getRelationModels(keyword);
  }

  @Operation(summary = "新增关系模型")
  @PostMapping
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public RelationModel newRelationModel(@RequestParam String name) {

    return relationModelService.newRelationModel(name);
  }

  @Operation(summary = "修改关系模型")
  @PutMapping("{id}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public RelationModel updateRelationModel(@PathVariable Long id, @RequestParam String name) {

    return relationModelService.updateRelationModel(id, name);
  }

  @Operation(summary = "删除关系模型")
  @DeleteMapping("{id}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void deleteRelationModel(@PathVariable Long id) {

    relationModelService.deleteRelationModel(id);
  }
}
