package com.singhand.sr.graphservice.bizbatchservice.tasklet;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.singhand.sr.graphservice.bizbatchservice.importer.vertex.VertexImporter;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem.PropertyItem;
import com.singhand.sr.graphservice.bizbatchservice.model.ImportVertexItem.VertexItem;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EdgeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyValueRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

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

  private final List<VertexImporter> vertexImporters;

  private final PropertyValueRepository propertyValueRepository;

  private final PlatformTransactionManager bizTransactionManager;

  private final EdgeRepository edgeRepository;

  public ImportVertexTasklet(@Value("${minio.bucket}") String bucket, MinioClient minioClient,
      VertexRepository vertexRepository, VertexService vertexService,
      OntologyRepository ontologyRepository,
      OntologyPropertyRepository ontologyPropertyRepository,
      RelationModelRepository relationModelRepository, List<VertexImporter> vertexImporters,
      PropertyValueRepository propertyValueRepository,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager,
      EdgeRepository edgeRepository) {

    this.bucket = bucket;
    this.minioClient = minioClient;
    this.vertexRepository = vertexRepository;
    this.vertexService = vertexService;
    this.ontologyRepository = ontologyRepository;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
    this.relationModelRepository = relationModelRepository;
    this.vertexImporters = vertexImporters;
    this.propertyValueRepository = propertyValueRepository;
    this.bizTransactionManager = bizTransactionManager;
    this.edgeRepository = edgeRepository;
  }

  @Override
  public RepeatStatus execute(@Nonnull StepContribution stepContribution,
      @Nonnull ChunkContext chunkContext) throws Exception {

    final var stepContext = stepContribution.getStepExecution();
    final var executionContext = stepContext.getJobExecution().getExecutionContext();
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

    try {
      final var extName = FileNameUtil.extName(tempFilename).toLowerCase();

      final var importer = vertexImporters.stream()
          .filter(it -> it.supports(tempFilename))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("不支持的文件格式：" + extName));

      final var importData = importer.importFromFile(tempFilename);
      log.info("文件读取完成，开始导入数据......");

      log.info("开始导入实体......实体总数：{}", CollUtil.size(importData.getEntities()));
      final var vertexMap = importVertices(importData);
      log.info("已成功导入 {} 个实体", vertexMap.size());

      log.info("开始导入关系......关系总数：{}", CollUtil.size(importData.getRelations()));
      final var edges = importRelations(importData);
      log.info("已成功导入 {} 条关系", edges.size());
    } catch (Exception e) {
      log.error("导入数据出现异常", e);
    } finally {
      log.info("清除临时目录和文件，{} ...", tempFilename);
      FileUtil.del(tempFilename);
      log.info("清除临时目录和文件成功！");
    }

    executionContext.putString("message", "已成功导入实体数据");

    return RepeatStatus.FINISHED;
  }

  /**
   * 导入关系
   *
   * @param importData 导入数据
   * @return 关系
   */
  private @Nonnull List<Edge> importRelations(@Nonnull ImportVertexItem importData) {

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

      final var existsEdge = edgeRepository
          .existsByNameAndInVertexAndOutVertexAndScope(
              relation.getName(), inVertex, outVertex, "default");

      if (existsEdge) {
        return;
      }

      final var request = new NewEdgeRequest();
      request.setName(relation.getName());
      log.info("正在导入关系：name={}, inVertex={}, outVertex={}", relation.getName(),
          inVertex.getName(), outVertex.getName());
      final var transaction = new TransactionTemplate(bizTransactionManager);
      transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      final var edge = transaction.execute(status -> {
        final var in = vertexService.getVertex(inVertex.getID());
        final var out = vertexService.getVertex(outVertex.getID());
        if (in.isEmpty() || out.isEmpty()) {
          return null;
        }
        return vertexService.newEdge(in.get(), out.get(), request);
      });
      if (null != edge) {
        edges.add(edge);
      }
    });

    return edges;
  }

  /**
   * 导入实体
   *
   * @param importData 导入数据
   * @return 实体
   */
  private @Nonnull Map<String, Vertex> importVertices(@Nonnull ImportVertexItem importData) {

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

              final var batchRequests = new ArrayList<NewPropertyRequest>();
              it.getValue().forEach(value -> {
                final var md5 = MD5.create().digestHex(value);
                final var existsValue = propertyValueRepository
                    .findByProperty_Vertex_IDAndProperty_KeyAndMd5(
                        vertex.getID(), it.getKey(), md5);

                if (existsValue.isPresent()) {
                  return;
                }
                final var request = new NewPropertyRequest();
                request.setKey(it.getKey());
                request.setValue(value);
                batchRequests.add(request);
              });

              if (CollUtil.isNotEmpty(batchRequests)) {
                final var transaction = new TransactionTemplate(bizTransactionManager);
                transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transaction.execute(status -> {
                  vertexService.getVertex(vertex.getID()).ifPresent(v ->
                      batchRequests.forEach(request -> {
                        log.info("正在导入实体属性：name={}, key={}",
                            vertex.getName(), request.getKey());
                        vertexService.newProperty(v, request);
                      }));
                  return true;
                });
              }
            });
      }
    });

    return vertexMap;
  }

  /**
   * 获取实体
   *
   * @param vertexItem 实体
   * @return 实体
   */
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
          final var transaction = new TransactionTemplate(bizTransactionManager);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          return transaction.execute(status -> {
            final var request = new NewVertexRequest();
            request.setName(name);
            request.setType(type);
            log.info("实体不存在，正在创建：name={}, type={}", name, type);
            return vertexService.newVertex(request);
          });
        });
  }
}
