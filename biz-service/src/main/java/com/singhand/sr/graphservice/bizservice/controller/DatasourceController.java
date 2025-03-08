package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizkeycloakmodel.helper.JwtHelper;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.reponse.OperationResponse;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizservice.client.feign.BizBatchServiceClient;
import com.singhand.sr.graphservice.bizservice.service.DatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @Autowired
  public DatasourceController(DatasourceService datasourceService,
      BizBatchServiceClient bizBatchServiceClient, DatasourceRepository datasourceRepository) {

    this.datasourceService = datasourceService;
    this.bizBatchServiceClient = bizBatchServiceClient;
    this.datasourceRepository = datasourceRepository;
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
        return bizBatchServiceClient.launchImportDatasourceJob(managedDatasource.getID(),
            username, uuid);
      }).toList();
    }
    throw new ResponseStatusException(HttpStatus.CONFLICT, "已经有数据源正在上传中！");
  }
}
