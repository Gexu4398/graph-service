package com.singhand.sr.graphservice.bizservice.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.UpdatePropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.response.GetVerticesResponseItem;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizkeycloakmodel.helper.JwtHelper;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
@RequestMapping("vertex")
@Tag(name = "实体管理")
@Validated
public class VertexController {

  private final VertexService vertexService;

  private final VertexRepository vertexRepository;

  private final OntologyRepository ontologyRepository;

  private final OntologyService ontologyService;

  private final PropertyRepository propertyRepository;

  @Autowired
  public VertexController(VertexService vertexService, VertexRepository vertexRepository,
      OntologyRepository ontologyRepository, OntologyService ontologyService,
      PropertyRepository propertyRepository) {

    this.vertexService = vertexService;
    this.vertexRepository = vertexRepository;
    this.ontologyRepository = ontologyRepository;
    this.ontologyService = ontologyService;
    this.propertyRepository = propertyRepository;
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
      @RequestParam(name = "keyValue", required = false, defaultValue = "") Set<String> keyValues,
      @RequestParam(defaultValue = "true") boolean recursive,
      Pageable pageable) {

    var filteredTypes = types;
    if (recursive) {
      filteredTypes = CollUtil.isEmpty(filteredTypes) ?
          filteredTypes : ontologyService.getAllSubOntologies(types);
    }

    final var page = vertexService.getVertices(keyword, filteredTypes,
        keyValues.stream().map(it -> {
          final var strings = StrUtil.split(it, ':', 2);
          return Map.entry(strings.get(0), strings.get(1));
        }).collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
        pageable);

    final var content = page.getContent().stream().map(GetVerticesResponseItem::new).toList();

    return new PageImpl<>(content, pageable, page.getTotalElements());
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

  @Operation(summary = "添加实体属性")
  @PostMapping(path = "{id}/property")
  @SneakyThrows
  @Transactional("bizTransactionManager")
  public void newVertexProperty(@PathVariable String id, @RequestParam String key,
      @Valid @RequestBody NewPropertyRequest newPropertyRequest) {

    final var vertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在！"));

    if (propertyRepository.findByVertexAndKey(vertex, key).isPresent()) {
      final var property = propertyRepository.findByVertexAndKey(vertex, key).get();
      final var valueMd5 = MD5.create().digestHex(newPropertyRequest.getValue());

      final var value = property.getValues()
          .stream()
          .filter(it -> it.getMd5().equals(valueMd5))
          .findFirst();

      if (value.isPresent()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "该属性值已存在！");
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
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在！"));

    final var username = JwtHelper.getUsername();

    requests.forEach(request -> {
      if (propertyRepository.findByVertexAndKey(vertex, request.getKey()).isPresent()) {
        final var property = propertyRepository.findByVertexAndKey(vertex,
            request.getKey()).get();
        final var valueMd5 = MD5.create().digestHex(request.getNewValue());
        final var value = property.getValues()
            .stream()
            .filter(it -> it.getMd5().equals(valueMd5))
            .findFirst();
        if (value.isPresent() &&
            !request.getNewValue().equals(request.getOldValue())) {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "该属性值已存在！");
        }
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode请为为md5!");
    }

    final var vertex = vertexService.getVertex(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在！"));

    vertexService.deleteProperty(vertex, key, value, mode);
  }
}
