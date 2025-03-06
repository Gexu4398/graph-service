package com.singhand.sr.graphservice.bizservice.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.model.response.GetVerticesResponseItem;
import com.singhand.sr.graphservice.bizgraph.service.OntologyService;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @Autowired
  public VertexController(VertexService vertexService, VertexRepository vertexRepository,
      OntologyRepository ontologyRepository, OntologyService ontologyService) {

    this.vertexService = vertexService;
    this.vertexRepository = vertexRepository;
    this.ontologyRepository = ontologyRepository;
    this.ontologyService = ontologyService;
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
}
