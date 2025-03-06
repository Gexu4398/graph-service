package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
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
}
