package com.singhand.sr.graphservice.bizbatchservice.tasklet;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem.PropertyItem;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem.VertexItem;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Slf4j
public class ImportVertexTasklet implements Tasklet {

  private final String bucket;

  private final MinioClient minioClient;

  private final VertexRepository vertexRepository;

  private final VertexService vertexService;

  private final OntologyRepository ontologyRepository;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  private final RelationModelRepository relationModelRepository;

  public ImportVertexTasklet(@Value("${minio.bucket}") String bucket, MinioClient minioClient,
      VertexRepository vertexRepository, VertexService vertexService,
      OntologyRepository ontologyRepository,
      OntologyPropertyRepository ontologyPropertyRepository,
      RelationModelRepository relationModelRepository) {

    this.bucket = bucket;
    this.minioClient = minioClient;
    this.vertexRepository = vertexRepository;
    this.vertexService = vertexService;
    this.ontologyRepository = ontologyRepository;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
    this.relationModelRepository = relationModelRepository;
  }

  @Override
  public RepeatStatus execute(@Nonnull StepContribution stepContribution,
      @Nonnull ChunkContext chunkContext) throws Exception {

    final var stepContext = stepContribution.getStepExecution();
    final var url = stepContext.getJobParameters().getString("url", "");
    final var object = StrUtil.subAfter(url, bucket, true);

    if (StrUtil.isBlank(url)) {
      throw new RuntimeException("文件路径为空");
    }

    final var tempFilename = Path.of(System.getProperty("java.io.tmpdir"),
        UUID.randomUUID() + "." + FileNameUtil.extName(object)).toString();

    log.info("正在导入数据源.......... 文件名={}", object);

    minioClient.downloadObject(DownloadObjectArgs.builder()
        .bucket(bucket)
        .object(object)
        .overwrite(true)
        .filename(tempFilename)
        .build());

    final var extName = FileNameUtil.extName(tempFilename).toLowerCase();

    if (!StrUtil.equals(extName, "json")) {
      throw new RuntimeException("文件格式错误");
    }

    final var objectMapper = new ObjectMapper();
    final var importData = objectMapper.readValue(new File(tempFilename), ImportVertexItem.class);

    try {
      final var vertexMap = importVertices(importData);
      log.info("已成功导入 {} 个实体", vertexMap.size());
      final var edges = importRelations(importData);
      log.info("已成功导入 {} 条关系", edges.size());
    } finally {
      FileUtil.del(tempFilename);
    }

    return RepeatStatus.FINISHED;
  }

  private List<Edge> importRelations(@Nonnull ImportVertexItem importData) {

    final var edges = new LinkedList<Edge>();

    if (CollUtil.isEmpty(importData.getRelations())) {
      return edges;
    }

    importData.getRelations().forEach(relation -> {
      final var exists = relationModelRepository.existsByName(relation.getName());
      if (!exists) {
        log.error("关系模型不存在：{}", relation.getName());
        return;
      }

      final var inVertex = getVertex(relation.getInVertex());
      final var outVertex = getVertex(relation.getOutVertex());
      if (null == inVertex || null == outVertex) {
        return;
      }

      final var request = new NewEdgeRequest();
      request.setName(relation.getName());
      final var edge = vertexService.newEdge(inVertex, outVertex, request);
      edges.add(edge);
    });

    return edges;
  }

  private Map<String, Vertex> importVertices(@Nonnull ImportVertexItem importData) {

    final var vertexMap = new HashMap<String, Vertex>();

    if (CollUtil.isEmpty(importData.getEntities())) {
      return vertexMap;
    }

    importData.getEntities().forEach(entity -> {
      final var vertex = getVertex(entity);

      if (null == vertex) {
        return;
      }

      vertexMap.put(vertex.getID(), vertex);

      if (CollUtil.isNotEmpty(entity.getProperties())) {
        final var properties = entity.getProperties()
            .stream()
            .collect(Collectors.toMap(PropertyItem::getKey, PropertyItem::getValue));
        properties.entrySet()
            .stream()
            .filter(it -> CollUtil.isNotEmpty(it.getValue()))
            .forEach(it -> {
              final var exists = ontologyPropertyRepository
                  .existsByOntology_NameAndName(vertex.getType(), it.getKey());

              if (!exists) {
                log.error("本体属性不存在：type={}, key={}", vertex.getType(), it.getKey());
                return;
              }

              it.getValue().forEach(value -> {
                final var request = new NewPropertyRequest();
                request.setKey(it.getKey());
                request.setValue(value);
                vertexService.newProperty(vertex, request);
              });
            });
      }
    });

    return vertexMap;
  }

  private Vertex getVertex(@Nonnull VertexItem vertexItem) {

    final var name = vertexItem.getName();
    final var type = vertexItem.getType();

    if (StrUtil.isBlank(name)) {
      log.error("实体必填项有缺失：name={}", name);
      return null;
    }

    if (StrUtil.isBlank(type)) {
      log.error("实体必填项有缺失：type={}", type);
      return null;
    }

    final var exists = ontologyRepository.existsByName(type);
    if (!exists) {
      log.error("本体不存在：type={}", type);
      return null;
    }

    return vertexRepository.findByNameAndType(name, type)
        .orElseGet(() -> {
          final var request = new NewVertexRequest();
          request.setName(name);
          request.setType(type);
          return vertexService.newVertex(request);
        });
  }
}
