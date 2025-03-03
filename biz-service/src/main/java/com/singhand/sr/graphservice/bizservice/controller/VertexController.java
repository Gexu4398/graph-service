package com.singhand.sr.graphservice.bizservice.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.neo4j.VertexNode;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.PropertyValueRepository;
import com.singhand.sr.graphservice.bizmodel.repository.neo4j.VertexNodeRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
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
@Tag(name = "实体和实体的关系")
@Validated
public class VertexController {

  private final VertexService vertexService;

  private final VertexNodeRepository vertexNodeRepository;

  private final PropertyValueRepository propertyValueRepository;

  @Autowired
  public VertexController(VertexService vertexService, VertexNodeRepository vertexNodeRepository,
      PropertyValueRepository propertyValueRepository) {

    this.vertexService = vertexService;
    this.vertexNodeRepository = vertexNodeRepository;
    this.propertyValueRepository = propertyValueRepository;
  }

  @Operation(summary = "获取实体详情")
  @GetMapping("{id}")
  @SneakyThrows
  public VertexNode getVertex(@PathVariable String id) {

    return vertexService.getVertex(id);
  }

  @Operation(summary = "获取实体列表")
  @GetMapping
  @SneakyThrows
  public Page<VertexNode> getVertices(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
      @RequestParam(name = "type", required = false, defaultValue = "") Set<String> types,
      @RequestParam(name = "keyValue", required = false, defaultValue = "") Set<String> keyValues,
      Pageable pageable) {

    final var properties = keyValues.stream().map(it -> {
      // RequestParam 传参不要使用逗号作为分隔符，因为根据 RFC 标准可能会被解析为数组，详见下文
      // 参考文献 https://github.com/spring-projects/spring-framework/issues/23820
      final var strings = StrUtil.split(it, ':', 2);
      return Map.entry(strings.get(0), strings.get(1));
    }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    return vertexService.getVertices(keyword, types, properties, pageable);
  }

  @Operation(summary = "插入实体")
  @PostMapping
  @SneakyThrows
  @Transactional("bizNeo4jTransactionManager")
  public VertexNode newVertex(@Valid @RequestBody NewVertexRequest request) {

    request.setName(StrUtil.trim(request.getName()));

    if (vertexNodeRepository.existsByNameAndType(request.getName(), request.getType())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "实体已存在");
    }

    return vertexService.newVertex(request);
  }

  @Operation(summary = "编辑实体")
  @PutMapping("{id}")
  @SneakyThrows
  @Transactional("bizNeo4jTransactionManager")
  public VertexNode updateVertex(@PathVariable String id,
      @Valid @RequestBody NewVertexRequest request) {

    request.setName(StrUtil.trim(request.getName()));

    final var vertex = vertexNodeRepository.findById(id).orElse(null);
    if (null == vertex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "实体不存在");
    } else if (StrUtil.equals(vertex.getName(), request.getName()) &&
        StrUtil.equals(vertex.getType(), request.getType())) {
      return vertexNodeRepository.save(vertex);
    }

    if (vertexNodeRepository.existsByNameAndType(request.getName(), request.getType())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "实体已存在");
    }

    return vertexService.updateVertex(vertex, request);
  }

  @Operation(summary = "添加实体属性")
  @PostMapping(path = "{id}/property")
  @SneakyThrows
  @Transactional("bizNeo4jTransactionManager")
  public void newVertexProperty(@PathVariable String id, @RequestParam String key,
      @Valid @RequestBody NewPropertyRequest newPropertyRequest) {

    final var vertex = vertexService.getVertex(id);

    final var md5 = MD5.create().digestHex(newPropertyRequest.getValue());
    final var exists = propertyValueRepository.existsByProperty_VertexNodeAndPropertyNode_NameAndMd5(
        vertex, key, md5);

    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "属性值已存在");
    }

    vertexService.newVertexProperty(vertex, key, newPropertyRequest);
  }
}
