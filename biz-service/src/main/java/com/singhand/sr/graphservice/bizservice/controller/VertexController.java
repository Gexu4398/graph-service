package com.singhand.sr.graphservice.bizservice.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizgraph.model.request.ImportVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEvidenceRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdateEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.response.GetVerticesResponseItem;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizkeycloakmodel.helper.JwtHelper;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.reponse.OperationResponse;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import com.singhand.sr.graphservice.bizservice.client.feign.BizBatchServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("vertex")
@Tag(name = "实体管理")
@Validated
public class VertexController {

  private final VertexService vertexService;

  private final VertexRepository vertexRepository;

  private final OntologyRepository ontologyRepository;

  private final OntologyService ontologyService;

  private final PropertyRepository propertyRepository;

  private final BizBatchServiceClient bizBatchServiceClient;

  @Autowired
  public VertexController(VertexService vertexService, VertexRepository vertexRepository,
      OntologyRepository ontologyRepository, OntologyService ontologyService,
      PropertyRepository propertyRepository, BizBatchServiceClient bizBatchServiceClient) {

    this.vertexService = vertexService;
    this.vertexRepository = vertexRepository;
    this.ontologyRepository = ontologyRepository;
    this.ontologyService = ontologyService;
    this.propertyRepository = propertyRepository;
    this.bizBatchServiceClient = bizBatchServiceClient;
  }

  @Operation(summary = "获取实体详情")
  @GetMapping(path = "{id}")
  public Vertex getVertex(@PathVariable String id) {

    return vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
  }

  @Operation(summary = "查询实体")
  @GetMapping
  public Page<GetVerticesResponseItem> getVertices(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
      @RequestParam(name = "type", required = false, defaultValue = "") Set<String> types,
      @RequestParam(required = false, defaultValue = "false") boolean useEs,
      @RequestParam(defaultValue = "true") boolean recursive,
      Pageable pageable) {

    final var filteredTypes = new HashSet<>(types);
    if (recursive && CollUtil.isNotEmpty(filteredTypes)) {
      filteredTypes.addAll(ontologyService.getAllSubOntologies(filteredTypes));
    }

    final var page = vertexService.getVertices(keyword, filteredTypes, useEs, pageable);

    final var content = page.getContent().stream().map(GetVerticesResponseItem::new).toList();

    return new PageImpl<>(content, pageable, page.getTotalElements());
  }

  @Operation(summary = "查询关系")
  @GetMapping("edge")
  public Page<Edge> getEdges(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
      @RequestParam(name = "name", required = false, defaultValue = "") String name,
      Pageable pageable) {

    return vertexService.getEdges(keyword, name, pageable);
  }

  @Operation(summary = "新增实体")
  @PostMapping
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Vertex newVertex(@Valid @RequestBody NewVertexRequest request) {

    request.setName(StrUtil.trim(request.getName()));

    final var existsOntology = ontologyRepository.existsByName(request.getType());
    if (!existsOntology) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在");
    }

    final var existsVertex = vertexRepository
        .existsByNameAndType(request.getName(), request.getType());
    if (existsVertex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "实体已存在");
    }

    return vertexService.newVertex(request);
  }

  @Operation(summary = "修改实体")
  @PutMapping("{id}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public Vertex updateVertex(@PathVariable String id, @RequestParam String name) {

    return vertexService.updateVertex(id, name);
  }

  @Operation(summary = "删除实体")
  @DeleteMapping("{id}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void deleteVertex(@PathVariable String id) {

    vertexService.deleteVertex(id);
  }

  @Operation(summary = "批量删除实体")
  @DeleteMapping("batch")
  @Transactional("bizTransactionManager")
  public void deleteVertices(@RequestBody List<String> vertexIds) {

    vertexService.deleteVertices(vertexIds);
  }

  @Operation(summary = "添加实体属性")
  @PostMapping(path = "{id}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void newVertexProperty(@PathVariable String id, @RequestParam String key,
      @Valid @RequestBody NewPropertyRequest newPropertyRequest) {

    final var vertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var ontology = ontologyRepository.findByName(vertex.getType())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var ontologyProperty = ontologyService.getProperty(ontology, key)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体属性不存在"));

    if (propertyRepository.findByVertexAndKey(vertex, key).isPresent()) {
      final var property = propertyRepository.findByVertexAndKey(vertex, key).get();
      if (!ontologyProperty.isMultiValue() && CollUtil.isNotEmpty(property.getValues())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "属性已存在值，且该属性不支持多值");
      }
      final var valueMd5 = MD5.create().digestHex(newPropertyRequest.getValue());

      final var value = property.getValues()
          .stream()
          .filter(it -> it.getMd5().equals(valueMd5))
          .findFirst();

      if (value.isPresent()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "该属性值已存在");
      }
    }

    newPropertyRequest.setKey(key);
    newPropertyRequest.setCreator(JwtHelper.getUsername());

    vertexService.newProperty(vertex, newPropertyRequest);
  }

  @Operation(summary = "批量修改属性")
  @PutMapping("{id}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void updateProperties(@PathVariable String id,
      @Valid @RequestBody List<UpdatePropertyRequest> requests) {

    final var vertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var ontology = ontologyRepository.findByName(vertex.getType())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体不存在"));

    final var username = JwtHelper.getUsername();

    final var ontologyProperties = new HashMap<String, Boolean>();
    requests.forEach(request -> {
      if (null == ontologyProperties.get(request.getKey())) {
        ontologyService.getProperty(ontology, request.getKey())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本体属性不存在"));
        ontologyProperties.put(request.getKey(), true);
      }

      final var propertyOptional = propertyRepository.findByVertexAndKey(vertex, request.getKey());
      if (propertyOptional.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该属性不存在");
      }
      request.setCreator(username);
    });

    requests.forEach(request -> vertexService.updateProperty(vertex, request));
  }

  @Operation(summary = "删除实体属性")
  @DeleteMapping("{id}/property")
  @Transactional("bizTransactionManager")
  public void deleteVertexProperty(@PathVariable String id,
      @RequestParam String key,
      @RequestParam String value,
      @RequestParam(defaultValue = "raw") String mode) {

    if (mode.equals("raw")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode请为md5");
    }

    final var vertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexService.deletePropertyValue(vertex, key, value, mode);
  }

  @Operation(summary = "插入关系")
  @PostMapping("{id}/edge/{outId}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void newEdge(@PathVariable String id, @PathVariable String outId,
      @Valid @RequestBody List<NewEdgeRequest> requests) {

    if (id.equals(outId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "实体不能和自身建立关系");
    }

    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var username = JwtHelper.getUsername();

    requests.forEach(request -> {
      request.setCreator(username);
      vertexService.newEdge(inVertex, outVertex, request);
    });
  }

  @Operation(summary = "修改关系")
  @PutMapping("{id}/edge/{outId}")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void updateEdge(@PathVariable String id, @PathVariable String outId,
      @Valid @RequestBody UpdateEdgeRequest request) {

    if (id.equals(outId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "实体不能和自己建立关系");
    }

    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    Edge oldEdge;
    if (StrUtil.isNotBlank(request.getOldId())) {
      final var oldOutVertex = vertexService.getVertex(request.getOldId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
      oldEdge = vertexService
          .getEdge(request.getOldName(), inVertex, oldOutVertex, request.getScope())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));
    } else {
      oldEdge = vertexService
          .getEdge(request.getOldName(), inVertex, outVertex, request.getScope())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));
    }

    final var newEdge = vertexService
        .getEdge(request.getNewName(), inVertex, outVertex, request.getScope())
        .orElse(null);

    if (null != newEdge && newEdge.equals(oldEdge)) {
      request.setCreator(JwtHelper.getUsername());
      vertexService.updateEdge(oldEdge, request);
    } else {
      vertexService.deleteEdge(oldEdge);
      final var newEdgeRequest = new NewEdgeRequest();
      newEdgeRequest.setCreator(JwtHelper.getUsername());
      newEdgeRequest.setName(request.getNewName());
      newEdgeRequest.setContent(request.getContent());
      newEdgeRequest.setDatasourceId(request.getDatasourceId());
      newEdgeRequest.setFeatures(request.getFeatures());
      newEdgeRequest.setScope(request.getScope());
      vertexService.newEdge(inVertex, outVertex, newEdgeRequest);
    }
  }

  @Operation(summary = "删除关系")
  @DeleteMapping("{id}/edge/{outId}")
  @Transactional("bizTransactionManager")
  public void deleteEdge(@PathVariable String id, @PathVariable String outId,
      @RequestParam String name, @RequestParam(defaultValue = "default") String scope) {

    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    vertexService.getEdge(name, inVertex, outVertex, scope)
        .ifPresentOrElse(vertexService::deleteEdge, () -> {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在");
        });
  }

  @Operation(summary = "新增关系属性")
  @PostMapping(path = "{id}/edge/{outId}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void newEdgeProperty(@PathVariable String id, @PathVariable String outId,
      @RequestParam String name, @RequestParam(defaultValue = "default") String scope,
      @RequestParam String key, @Valid @RequestBody NewPropertyRequest newPropertyRequest) {

    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var edge = vertexService.getEdge(name, inVertex, outVertex, scope)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));
    newPropertyRequest.setKey(key);
    newPropertyRequest.setCreator(JwtHelper.getUsername());
    vertexService.newProperty(edge, newPropertyRequest);
  }

  @Operation(summary = "修改关系属性")
  @PutMapping("{id}/edge/{outId}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void updateEdgeProperties(@PathVariable String id, @PathVariable String outId,
      @RequestParam(defaultValue = "default") String scope, @RequestParam String name,
      @Valid @RequestBody List<UpdatePropertyRequest> requests) {

    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var edge = vertexService.getEdge(name, inVertex, outVertex, scope)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    final var username = JwtHelper.getUsername();
    requests.forEach(updatePropertyRequest -> {
      updatePropertyRequest.setCreator(username);
      vertexService.updateProperty(edge, updatePropertyRequest);
    });
  }

  @Operation(summary = "删除关系属性")
  @DeleteMapping("{id}/edge/{outId}/property")
  @Transactional("bizTransactionManager")
  public void deleteEdgeProperty(@PathVariable String id, @PathVariable String outId,
      @RequestParam String key, @RequestParam String value, @RequestParam String name,
      @RequestParam(defaultValue = "default") String scope,
      @RequestParam(defaultValue = "raw") String mode) {

    if (mode.equals("raw")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode请为md5!");
    }
    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var edge = vertexService.getEdge(name, inVertex, outVertex, scope)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));
    vertexService.deleteProperty(edge, key, value, mode);
  }

  @Operation(summary = "根据实体属性知识查询属性证据")
  @GetMapping("/{id}/property/evidence")
  public Page<Evidence> getEvidenceByPropertyValue(@PathVariable String id,
      @RequestParam String key, @RequestParam String value,
      @RequestParam(defaultValue = "raw") String mode, Pageable pageable) {

    if (mode.equals("raw")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode请为md5!");
    }

    final var vertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    return vertexService.getEvidences(vertex, key, value, mode, pageable);
  }

  @Operation(summary = "根据关系属性知识查询属性证据")
  @GetMapping("{id}/edge/{outId}/property/evidence")
  public Page<Evidence> getEvidenceByEdgePropertyValue(@PathVariable String id,
      @PathVariable String outId, @RequestParam String name,
      @RequestParam String key, @RequestParam String value,
      @RequestParam(defaultValue = "default") String scope,
      @RequestParam(defaultValue = "raw") String mode, Pageable pageable) {

    if (mode.equals("raw")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode请为md5!");
    }
    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var edge = vertexService.getEdge(name, inVertex, outVertex, scope)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));

    return vertexService.getEvidences(edge, key, value, mode, pageable);
  }

  @Operation(summary = "为属性值添加证据")
  @PostMapping("{id}/property/evidence")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void addEvidenceByPropertyValue(@PathVariable String id, @RequestParam String key,
      @RequestParam String value, @RequestBody NewEvidenceRequest newEvidenceRequest) {

    final var vertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var property = vertexService.getProperty(vertex, key)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性不存在"));
    final var propertyValue = vertexService.getPropertyValue(property, value)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "属性值不存在"));
    newEvidenceRequest.setCreator(JwtHelper.getUsername());
    vertexService.addEvidence(propertyValue, newEvidenceRequest);
  }

  @Operation(summary = "为关系添加证据")
  @PostMapping("{id}/edge/{outId}/evidence")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void addEvidencesByEdge(@PathVariable String id, @PathVariable String outId,
      @RequestParam String name, @RequestParam(defaultValue = "default") String scope,
      @RequestBody NewEvidenceRequest newEvidenceRequest) {

    final var inVertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var outVertex = vertexService.getVertex(outId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    final var edge = vertexService.getEdge(name, inVertex, outVertex, scope)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "关系不存在"));
    newEvidenceRequest.setCreator(JwtHelper.getUsername());
    vertexService.addEvidence(edge, newEvidenceRequest);
  }

  @Operation(summary = "获取导入模板")
  @PostMapping("import/template")
  @SneakyThrows
  public ResponseEntity<InputStreamResource> getImportTemplate() {

    @Cleanup final var inputStream = ResourceUtil.getStream("importer/example.json");
    final var inputStreamResource = new InputStreamResource(inputStream);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template.json")
        .contentType(MediaType.APPLICATION_JSON)
        .body(inputStreamResource);
  }

  @Operation(summary = "导入实体")
  @PostMapping("import")
  @SneakyThrows
  public OperationResponse importVertex(@RequestBody ImportVertexRequest importVertexRequest) {

    return bizBatchServiceClient.launchImportVertexJob(importVertexRequest);
  }

  @Operation(summary = "统计实体关系")
  @GetMapping("statistics/edge")
  public Long getStatisticsEdge() {

    return vertexService.countEdges();
  }
}
