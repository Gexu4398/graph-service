package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizgraph.model.request.DeletePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateOntologyPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Ontology;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.jpa.RelationInstance;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.OntologyNode;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OntologyController {

  private final OntologyService ontologyService;

  private final RelationModelRepository relationModelRepository;

  private final VertexService vertexService;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  @Autowired
  public OntologyController(OntologyService ontologyService,
      RelationModelRepository relationModelRepository, VertexService vertexService,
      OntologyPropertyRepository ontologyPropertyRepository) {

    this.ontologyService = ontologyService;
    this.relationModelRepository = relationModelRepository;
    this.vertexService = vertexService;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
  }

  @Operation(summary = "获取本体详情")
  @GetMapping(path = "{id}")
  public Ontology getOntology(@PathVariable Long id) {

    return ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));
  }

  @Operation(summary = "查询本体")
  @GetMapping
  public Page<Ontology> getOntologies(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
      Pageable pageable) {

    return ontologyService.getOntologies(keyword, pageable);
  }

  @Operation(summary = "新增本体")
  @PostMapping
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Ontology newOntology(@RequestParam String name,
      @RequestParam(required = false, defaultValue = "") Long parentId) {

    return ontologyService.newOntology(name, parentId);
  }

  @Operation(summary = "修改本体")
  @PutMapping("{id}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Ontology updateOntology(@PathVariable Long id, @RequestParam String name) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var oldName = ontology.getName();

    if (ontology.getName().equals(name)) {
      return ontology;
    }

    final var managedOntology = ontologyService.updateOntology(ontology, name);
    vertexService.batchUpdateVertex(oldName, name);
    return managedOntology;
  }

  @Operation(summary = "删除本体")
  @DeleteMapping("{id}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void deleteOntology(@PathVariable Long id) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var ontologyNames = ontologyService.getAllSubOntologies(Set.of(ontology.getName()));

    ontologyService.deleteOntology(id);

    vertexService.batchDeleteVertex(ontologyNames);
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
  public void newProperty(@PathVariable Long id,
      @Valid @RequestBody NewOntologyPropertyRequest request) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    ontologyService.newOntologyProperty(ontology, request);
  }

  @Operation(summary = "修改本体属性")
  @PutMapping("{id}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void updateProperty(@PathVariable Long id,
      @Valid @RequestBody UpdateOntologyPropertyRequest request) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    ontologyService.updateOntologyProperty(ontology, request);

    vertexService.batchUpdateVertexProperty(ontology.getName(), request.getOldName(),
        request.getNewName());
  }

  @Operation(summary = "删除本体属性")
  @DeleteMapping("{id}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void deleteProperties(@PathVariable Long id,
      @Valid @RequestBody DeletePropertyRequest request) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var properties = ontologyPropertyRepository.findAllById(request.getPropertyIds());
    final var keys = properties.stream()
        .map(OntologyProperty::getName)
        .collect(Collectors.toSet());

    ontologyService.deleteOntologyProperties(ontology, request);

    keys.forEach(it -> vertexService.batchDeleteVertexProperty(ontology.getName(), it));
  }

  @Operation(summary = "获取本体树")
  @GetMapping(path = "tree")
  @SneakyThrows
  public List<OntologyNode> getTree(@RequestParam(required = false, defaultValue = "") Long id) {

    return ontologyService.getTree(id);
  }

  @Operation(summary = "获取本体关系")
  @GetMapping("{id}/relation")
  @SneakyThrows
  public Page<RelationInstance> getRelations(@PathVariable Long id, Pageable pageable) {

    final var ontology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    return ontologyService.getRelations(ontology, pageable);
  }

  @Operation(summary = "新增本体关系")
  @PostMapping("{id}/relation/{outId}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public RelationInstance newRelation(@PathVariable Long id, @PathVariable Long outId,
      @RequestParam String name) {

    final var inOntology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var outOntology = ontologyService.getOntology(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var relationModel = relationModelRepository.findByName(name)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    return ontologyService.newRelation(relationModel.getName(), inOntology, outOntology);
  }

  @Operation(summary = "修改本体关系")
  @PutMapping("{id}/relation/{outId}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public RelationInstance updateRelation(@PathVariable Long id, @PathVariable Long outId,
      @RequestParam String name, @RequestParam String newName) {

    final var inOntology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var outOntology = ontologyService.getOntology(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var relationModel = relationModelRepository.findByName(name)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系模型不存在"));

    final var relationModel_2 = relationModelRepository.findByName(newName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系模型不存在"));

    final var relationInstance = ontologyService.updateRelation(relationModel.getName(),
        relationModel_2.getName(), inOntology, outOntology);

    vertexService.batchUpdateVertexEdge(name, newName, inOntology.getName(), outOntology.getName());

    return relationInstance;
  }

  @Operation(summary = "删除本体关系")
  @DeleteMapping("{id}/relation/{outId}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void deleteRelation(@PathVariable Long id, @PathVariable Long outId,
      @RequestParam String name) {

    final var inOntology = ontologyService.getOntology(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var outOntology = ontologyService.getOntology(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var relationModel = relationModelRepository.findByName(name)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系模型不存在"));

    ontologyService.deleteRelation(relationModel.getName(), inOntology, outOntology);

    vertexService.batchDeleteVertexEdge(name, inOntology.getName(), outOntology.getName());
  }
}
