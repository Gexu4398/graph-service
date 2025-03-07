package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @Autowired
  public OntologyController(OntologyService ontologyService) {

    this.ontologyService = ontologyService;
  }

  @Operation(summary = "获取本体详情")
  @GetMapping(path = "{id}")
  public Ontology getOntology(@PathVariable Long id) {

    return ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));
  }

  @Operation(summary = "新增本体")
  @PostMapping
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Ontology newOntology(@RequestParam String name,
      @RequestParam(required = false, defaultValue = "") Long parentId) {

    return ontologyService.newOntology(name, parentId);
  }

  @Operation(summary = "获取本体属性")
  @GetMapping("{id}/property")
  @SneakyThrows
  public Page<OntologyProperty> getProperties(@PathVariable Long id, Pageable pageable) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    return ontologyService.getProperties(ontology, pageable);
  }

  @Operation(summary = "新增本体属性")
  @PostMapping("{id}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void newProperty(@PathVariable Long id, @RequestParam String propertyName) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    ontologyService.newOntologyProperty(ontology, propertyName);
  }

  @Operation(summary = "获取本体树")
  @GetMapping(path = "tree")
  @SneakyThrows
  public List<OntologyNode> getTree(@RequestParam(required = false, defaultValue = "") Long id) {

    return ontologyService.getTree(id);
  }
}
