package com.singhand.sr.graphservice.bizservice.controller;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.NewOntologyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.dto.OntologyTreeDTO;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.OntologyNodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("ontology")
@Tag(name = "本体管理")
@Validated
public class OntologyController {

  private final OntologyService ontologyService;

  private final OntologyNodeRepository ontologyNodeRepository;

  @Autowired
  public OntologyController(OntologyService ontologyService,
      OntologyNodeRepository ontologyNodeRepository) {

    this.ontologyService = ontologyService;
    this.ontologyNodeRepository = ontologyNodeRepository;
  }

  @Operation(summary = "获取本体")
  @GetMapping("{id}")
  @SneakyThrows
  public OntologyNode getOntology(@PathVariable String id) {

    return ontologyService.getOntology(id);
  }

  @Operation(summary = "查询本体")
  @GetMapping
  @SneakyThrows
  public Page<OntologyNode> getOntologies(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
      Pageable pageable) {

    return ontologyService.getOntologies(keyword, pageable);
  }

  @Operation(summary = "获取本体树")
  @GetMapping("tree")
  @SneakyThrows
  public List<OntologyTreeDTO> getOntologyTree() {

    return ontologyService.getOntologyTree();
  }

  @Operation(summary = "添加本体")
  @PostMapping
  @SneakyThrows
  @Transactional("bizNeo4jTransactionManager")
  public OntologyNode newOntology(@Valid @RequestBody NewOntologyRequest request) {

    request.setName(StrUtil.trim(request.getName()));

    if (ontologyNodeRepository.existsByName(request.getName())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体已存在");
    }

    return ontologyService.newOntology(request);
  }

  @Operation(summary = "修改本体")
  @PutMapping("{id}")
  @SneakyThrows
  @Transactional("bizNeo4jTransactionManager")
  public OntologyNode updateOntology(@PathVariable String id,
      @Valid @RequestBody NewOntologyRequest request) {

    final var ontologyNode = ontologyService.getOntology(id);

    // 校验是否修改了本体，未修改直接返回
    final var currentParentId = Optional.ofNullable(ontologyNode.getParent())
        .map(OntologyNode::getId)
        .orElse(null);

    if (StrUtil.equals(request.getName(), ontologyNode.getName()) &&
        StrUtil.equals(request.getParentId(), currentParentId)) {
      return ontologyNode;
    }

    final var exists = ontologyNodeRepository.existsByName(request.getName());

    // 校验名称是否存在
    if (!StrUtil.equals(ontologyNode.getName(), request.getName()) && exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "本体已存在");
    }

    return ontologyService.updateOntology(ontologyNode, request);
  }

  @Operation(summary = "删除本体")
  @DeleteMapping("{id}")
  @SneakyThrows
  public void deleteOntology(@PathVariable String id) {

    final var ontologyNode = ontologyService.getOntology(id);

    ontologyService.deleteOntology(ontologyNode);
  }
}
