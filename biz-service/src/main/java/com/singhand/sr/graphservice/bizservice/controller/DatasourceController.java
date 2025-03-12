package com.singhand.sr.graphservice.bizservice.controller;

import cn.hutool.core.bean.BeanUtil;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizkeycloakmodel.helper.JwtHelper;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.model.reponse.OperationResponse;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import com.singhand.sr.graphservice.bizservice.client.feign.BizBatchServiceClient;
import com.singhand.sr.graphservice.bizservice.model.response.DatasourceResponse;
import com.singhand.sr.graphservice.bizservice.service.DatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "数据源管理")
@RestController
@RequestMapping("datasource")
@Slf4j
public class DatasourceController {

  private final DatasourceService datasourceService;

  private final BizBatchServiceClient bizBatchServiceClient;

  private final DatasourceRepository datasourceRepository;

  private final VertexService vertexService;

  private final VertexRepository vertexRepository;

  @Autowired
  public DatasourceController(DatasourceService datasourceService,
      BizBatchServiceClient bizBatchServiceClient, DatasourceRepository datasourceRepository,
      VertexService vertexService, VertexRepository vertexRepository) {

    this.datasourceService = datasourceService;
    this.bizBatchServiceClient = bizBatchServiceClient;
    this.datasourceRepository = datasourceRepository;
    this.vertexService = vertexService;
    this.vertexRepository = vertexRepository;
  }

  @GetMapping("/{id}")
  @Operation(summary = "查看数据源")
  public DatasourceResponse getDatasource(@PathVariable Long id,
      @RequestParam(required = false, defaultValue = "true") boolean hideHtml) {

    final var datasource = datasourceService.getDatasource(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));

    final var datasourceResponse = new DatasourceResponse();
    BeanUtil.copyProperties(datasource, datasourceResponse);

    if (hideHtml) {
      return datasourceResponse;
    }

    return datasourceService.updateImageElements(datasourceResponse);
  }

  @GetMapping
  @Operation(summary = "查询数据源列表")
  @SneakyThrows
  public Page<DatasourceResponse> getDataSources(
      @RequestParam(name = "q", required = false, defaultValue = "") String keyword,
      @RequestParam(name = "contentType", required = false, defaultValue = "") Set<String> contentTypes,
      @RequestParam(defaultValue = "true") boolean hideHtml,
      Pageable pageable) {

    final var page = datasourceService.getDataSources(keyword, contentTypes, pageable);

    if (hideHtml) {
      final var content = page.stream()
          .map(it -> {
            final var datasourceResponse = new DatasourceResponse();
            BeanUtil.copyProperties(it, datasourceResponse);
            return datasourceResponse;
          }).toList();
      return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    final var content = page.getContent().stream()
        .map(it -> {
          final var datasourceResponse = new DatasourceResponse();
          BeanUtil.copyProperties(it, datasourceResponse);
          datasourceService.updateImageElements(datasourceResponse);
          return datasourceResponse;
        })
        .collect(Collectors.toList());

    return new PageImpl<>(content, pageable, page.getTotalElements());
  }

  @PostMapping
  @Operation(summary = "导入数据源")
  @SneakyThrows
  public Collection<OperationResponse> newDatasource(
      @Valid @RequestBody Collection<Datasource> datasources) {

    final var username = JwtHelper.getUsername();
    if (bizBatchServiceClient.getJobs(username, "importDatasourceJob")
        .stream().noneMatch(it -> it.getExitCode().equals("UNKNOWN"))) {
      final var uuid = UUID.randomUUID().toString();
      return datasources.stream().map(datasource -> {
        datasource.setCreator(username);
        final var managedDatasource = datasourceService.newDatasource(datasource);
        datasourceRepository.flush();
        return bizBatchServiceClient
            .launchImportDatasourceJob(managedDatasource.getID(), username, uuid);
      }).toList();
    }
    throw new ResponseStatusException(HttpStatus.CONFLICT, "已经有数据源正在上传中");
  }

  @PostMapping("{id}/vertex/{vertexId}")
  @Operation(summary = "建立实体与数据源关联")
  @Transactional("bizTransactionManager")
  public Datasource attachVertex(@PathVariable Long id, @PathVariable String vertexId) {

    final var datasource = datasourceService.getDatasource(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));
    final var vertex = vertexService.getVertex(vertexId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));
    return datasourceService.attachVertex(datasource, vertex);
  }

  @DeleteMapping("{id}/vertex/{vertexId}")
  @Operation(summary = "解除实体与数据源关联")
  @Transactional("bizTransactionManager")
  public Datasource detachVertex(@PathVariable Long id, @PathVariable String vertexId) {

    final var datasource = datasourceService.getDatasource(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));

    final var vertex = vertexService.getVertex(vertexId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实体不存在"));

    return datasourceService.detachVertex(datasource, vertex);
  }

  @DeleteMapping("{id}")
  @Operation(summary = "删除数据源")
  @Transactional("bizTransactionManager")
  public void deleteDatasource(@PathVariable Long id) {

    datasourceService.deleteDatasource(id);
  }

  @GetMapping("{id}/vertex")
  @Operation(summary = "数据源关联实体列表")
  public Page<Vertex> getVertices(@PathVariable Long id, Pageable pageable) {

    final var datasource = datasourceService.getDatasource(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在"));

    return vertexRepository.findByDatasources_ID(datasource.getID(), pageable);
  }

  @GetMapping("importing")
  @Operation(summary = "查询正在导入的数据源")
  @SneakyThrows
  public Collection<OperationResponse> importingDataSources() {

    return bizBatchServiceClient.getJobs(JwtHelper.getUsername(), "importDatasourceJob");
  }
}
