package com.singhand.sr.graphservice.bizbatchservice.tasklet;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizbatchservice.client.feign.AiEvidenceExtractorClient;
import com.singhand.sr.graphservice.bizbatchservice.client.feign.AiTextExtractorClient;
import com.singhand.sr.graphservice.bizbatchservice.importer.helper.ExtractHelper;
import com.singhand.sr.graphservice.bizbatchservice.model.request.AiEvidenceExtractorRequest;
import com.singhand.sr.graphservice.bizbatchservice.model.request.AiTextExtractorRequest;
import com.singhand.sr.graphservice.bizbatchservice.model.response.AiEvidenceExtractorResponse;
import com.singhand.sr.graphservice.bizbatchservice.model.response.AiEvidenceExtractorResponse.Label;
import com.singhand.sr.graphservice.bizbatchservice.model.response.AiTextExtractorResponse;
import com.singhand.sr.graphservice.bizgraph.model.request.NewEdgeRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewPropertyRequest;
import com.singhand.sr.graphservice.bizgraph.model.request.NewVertexRequest;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.DatasourceContent;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.OntologyProperty;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Picture;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceContentRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EdgeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EvidenceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PictureRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@StepScope
@Slf4j
public class ImportDatasourceTasklet implements Tasklet {

  private final MinioClient minioClient;

  private final String bucket;

  private final DatasourceRepository datasourceRepository;

  private final Long id;

  private final PlatformTransactionManager bizTransactionManager;

  private final EntityManager bizEntityManager;

  private final PictureRepository pictureRepository;

  private final EvidenceRepository evidenceRepository;

  private final OntologyRepository ontologyRepository;

  private final AiEvidenceExtractorClient aiEvidenceExtractorClient;

  private final AiTextExtractorClient aiTextExtractorClient;

  private final VertexRepository vertexRepository;

  private final VertexService vertexService;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  private final RelationModelRepository relationModelRepository;

  private final EdgeRepository edgeRepository;

  private final ExtractHelper extractHelper;

  private final DatasourceContentRepository datasourceContentRepository;

  private final ChatModel chatModel;

  @Autowired
  public ImportDatasourceTasklet(MinioClient minioClient,
      @Value("${minio.bucket}") String bucket,
      @Value("#{jobParameters['id']}") Long id,
      DatasourceRepository datasourceRepository,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager,
      @Qualifier("bizEntityManager") EntityManager bizEntityManager,
      PictureRepository pictureRepository, EvidenceRepository evidenceRepository,
      OntologyRepository ontologyRepository,
      AiEvidenceExtractorClient aiEvidenceExtractorClient,
      AiTextExtractorClient aiTextExtractorClient, VertexRepository vertexRepository,
      VertexService vertexService, OntologyPropertyRepository ontologyPropertyRepository,
      RelationModelRepository relationModelRepository, EdgeRepository edgeRepository,
      ExtractHelper extractHelper, DatasourceContentRepository datasourceContentRepository,
      ChatModel chatModel) {

    this.minioClient = minioClient;
    this.bucket = bucket;
    this.datasourceRepository = datasourceRepository;
    this.id = id;
    this.bizTransactionManager = bizTransactionManager;
    this.bizEntityManager = bizEntityManager;
    this.pictureRepository = pictureRepository;
    this.evidenceRepository = evidenceRepository;
    this.ontologyRepository = ontologyRepository;
    this.aiEvidenceExtractorClient = aiEvidenceExtractorClient;
    this.aiTextExtractorClient = aiTextExtractorClient;
    this.vertexRepository = vertexRepository;
    this.vertexService = vertexService;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
    this.relationModelRepository = relationModelRepository;
    this.edgeRepository = edgeRepository;
    this.extractHelper = extractHelper;
    this.datasourceContentRepository = datasourceContentRepository;
    this.chatModel = chatModel;
  }

  @Override
  public RepeatStatus execute(@NonNull StepContribution stepContribution,
      @NonNull ChunkContext chunkContext) {

    final var stepContext = stepContribution.getStepExecution();
    final var url = stepContext.getJobParameters().getString("url", "");
    final var datasource = datasourceRepository.findById(id).orElseThrow();

    try {
      final var object = StrUtil.subAfter(StrUtil.isNotBlank(url) ? url : datasource.getUrl(),
          bucket,
          true);

      final var tempFilename = Path.of(System.getProperty("java.io.tmpdir"),
          UUID.randomUUID() + "." + FileNameUtil.extName(object)).toString();

      log.info("正在导入数据源.......... 文件名={}", object);
      updateDatasourceStatus(datasource, Datasource.STATUS_PROCESSING);

      minioClient.downloadObject(DownloadObjectArgs.builder()
          .bucket(bucket)
          .object(object)
          .overwrite(true)
          .filename(tempFilename)
          .build());

      final var datasourceContent = datasourceContentRepository
          .findByDatasource_ID(datasource.getID())
          .orElse(new DatasourceContent());

      datasource.attachContent(datasourceContent);

      final var paragraphs = extractHelper.extractFile(tempFilename, datasourceContent);

      // 此处要开辟新事务，否则会合并到最外层事务再提交，进而导致导入过程看不到数据源的问题。
      final var transaction = new TransactionTemplate(bizTransactionManager);
      transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      transaction.execute(status -> {
        datasourceRepository.save(datasource);
        return savePictures(datasource, datasourceContent);
      });

      textExtract(paragraphs, datasource);

      updateDatasourceStatus(datasource, Datasource.STATUS_SUCCESS);

      bizEntityManager.flush();

      return RepeatStatus.FINISHED;
    } catch (Exception e) {
      log.error("导入数据源时出现异常", e);
      updateDatasourceStatus(datasource, Datasource.STATUS_FAILURE);
      return RepeatStatus.FINISHED;
    }
  }

  private void updateDatasourceStatus(@Nonnull Datasource datasource,
      @Nonnull String status) {

    datasource.setStatus(status);

    // 创建新事务并立即提交状态变更
    final var transaction = new TransactionTemplate(bizTransactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.execute(transactionStatus -> datasourceRepository.save(datasource));
  }

  @SneakyThrows
  private void textExtract(List<String> paragraphs, @Nonnull Datasource datasource) {

    final var request = new AiTextExtractorRequest();
    request.setTexts(paragraphs);
    log.info("开始调用实体提取服务..........");
    final var response = aiTextExtractorClient.informationExtract(request);
    log.info("开始提取实体..........");
    importAiTextExtractorResult(response, datasource);

    final var evidenceRequest = new AiEvidenceExtractorRequest();
    evidenceRequest.setTexts(paragraphs);
    log.info("开始调用事件提取服务..........");
    final var evidenceResponse = aiEvidenceExtractorClient.propertyEdgeExtract(evidenceRequest);
    log.info("开始提取事件..........");
    importAiEvidenceExtractorResult(evidenceResponse, datasource);
  }

  private void importAiTextExtractorResult(AiTextExtractorResponse response,
      Datasource datasource) {

    importVertices(response, datasource);
    importAttributes(response, datasource);
    importRelationships(response, datasource);
  }

  private void importRelationships(@Nonnull AiTextExtractorResponse response,
      @Nonnull Datasource datasource) {

    final var relations = relationModelRepository.findAllNames();

    response.getRelations().forEach(relationship -> {
      if (!relations.contains(relationship.getRelation())) {
        log.warn("不能识别的关系类型：{}", relationship.getRelation());
        return;
      }

      final var inVertex = getVertex(relationship.getSubject(), relationship.getSubjectType());
      final var outVertex = getVertex(relationship.getObject(), relationship.getObjectType());

      if (null == inVertex) {
        log.warn("主语实体不存在......name={} type={}",
            relationship.getSubject(), relationship.getSubjectType());
        return;
      }
      if (null == outVertex) {
        log.warn("宾语实体不存在......name={} type={}",
            relationship.getObject(), relationship.getObjectType());
        return;
      }

      final var request = new NewEdgeRequest();
      request.setName(relationship.getRelation());
      request.setDatasourceId(datasource.getID());

      final var exists = edgeRepository.existsByNameAndInVertexAndOutVertexAndScope(
          relationship.getRelation(), inVertex, outVertex, "default");

      if (exists) {
        log.warn("关系已存在......name={} in={} out={}",
            relationship.getRelation(),
            String.format("%s(%s)", inVertex.getName(), inVertex.getType()),
            String.format("%s(%s)", outVertex.getName(), outVertex.getType()));
        return;
      }

      vertexService.newEdge(inVertex, outVertex, request);
    });
  }

  private void importAiEvidenceExtractorResult(@Nonnull AiEvidenceExtractorResponse response,
      @Nonnull Datasource datasource) {

    response.getLabels().forEach(it ->
        it.forEach((key, value) ->
            value.forEach(label -> {
              final var labelText = label.getText();
              final var properties = new HashMap<String, List<Label>>();
              final var relations = label.getRelations().entrySet()
                  .stream()
                  .map(entry -> {
                    final var stringBuilder = new StringBuilder();
                    for (final var entryValue : entry.getValue()) {
                      properties.put(entry.getKey(), entry.getValue());
                      stringBuilder.append(entryValue.getText());
                    }
                    return String.format("%s:%s", entry.getKey(), stringBuilder);
                  }).toList();
              final var content = String.format("事件类型：%s, 事件描述：%s, 属性：%s",
                  label.getEventType(), labelText, relations);
              log.warn("content: {}", content);

              final var question = String.format("%s，根据上下文组成一个名称，仅返回名称即可",
                  content);

              final var result = chatModel.call(question);
              final var pattern = Pattern.compile("</think>(.*?)$", Pattern.DOTALL);
              final var matcher = pattern.matcher(result);
              if (matcher.find()) {
                final var eventName = matcher.group(1).trim();

                final var request = new NewVertexRequest();
                request.setName(eventName);
                request.setType("事件");
                final var vertex = vertexService.newVertex(request);

                properties.forEach((k, v) ->
                    v.forEach(labelValue -> {
                      final var propertyRequest = new NewPropertyRequest();
                      propertyRequest.setKey(k);
                      propertyRequest.setValue(labelValue.getText());
                      propertyRequest.setDatasourceId(datasource.getID());
                      propertyRequest.setContent(labelValue.getText());
                      vertexService.newProperty(vertex, propertyRequest);
                    }));
              } else {
                log.warn("无法解析结果：{}", result);
              }
            })));
  }

  private void importVertices(@Nonnull AiTextExtractorResponse response,
      @Nonnull Datasource datasource) {

    if (CollUtil.isEmpty(response.getEntities())) {
      return;
    }

    final var types = ontologyRepository.findAllNames();

    response.getEntities()
        .forEach(entity -> {
          if (!types.contains(entity.getType())) {
            log.warn("不能识别的实体类型：{}", entity.getType());
          } else if (StrUtil.isNotBlank(entity.getName()) && StrUtil.isNotBlank(entity.getType())) {
            final var exists = vertexRepository
                .findByNameAndType(entity.getName(), entity.getType())
                .isPresent();
            if (exists) {
              return;
            }
            final var request = new NewVertexRequest();
            request.setName(entity.getName());
            request.setType(entity.getType());
            request.setDatasourceId(datasource.getID());
            request.setContent(response.getText());
            vertexService.newVertex(request);
          }
        });
  }

  private void importAttributes(@Nonnull AiTextExtractorResponse response,
      @Nonnull Datasource datasource) {

    final var ontologyPropertyMap = new HashMap<String, Set<String>>();

    response.getAttributes()
        .forEach(attribute -> {
          if (StrUtil.isBlank(attribute.getSubject())
              || StrUtil.isBlank(attribute.getSubjectType())
              || StrUtil.isBlank(attribute.getRelation())
              || StrUtil.isBlank(attribute.getObject())) {
            log.warn("必填项有缺失：name={} type={} key={} value={}", attribute.getSubject(),
                attribute.getSubjectType(), attribute.getRelation(), attribute.getObject());
            return;
          }

          final var vertexType = attribute.getSubjectType();
          if (CollUtil.isEmpty(ontologyPropertyMap.get(vertexType))) {
            final var ontologyProperties = ontologyPropertyRepository
                .findByOntology_Name(vertexType);
            final var propertyNames = ontologyProperties.stream()
                .map(OntologyProperty::getName)
                .collect(Collectors.toSet());
            ontologyPropertyMap.put(vertexType, propertyNames);
          }
          final var properties = ontologyPropertyMap.get(vertexType);
          if (!properties.contains(attribute.getRelation())) {
            log.warn("不能识别的属性类型：{}", attribute.getRelation());
            return;
          }
          final var vertex = vertexRepository.findByNameAndType(attribute.getSubject(), vertexType)
              .orElse(null);
          if (null == vertex) {
            log.warn("实体不存在：name={} type={}", attribute.getSubject(), vertexType);
            return;
          }
          final var request = new NewPropertyRequest();
          request.setKey(attribute.getRelation());
          request.setValue(attribute.getObject());
          request.setDatasourceId(datasource.getID());
          request.setContent(response.getText());
          vertexService.newProperty(vertex, request);
        });
  }

  @SneakyThrows
  private Set<String> savePictures(@Nonnull Datasource datasource,
      @Nonnull DatasourceContent datasourceContent) {

    final var document = Jsoup.parse(datasourceContent.getHtml());

    return document
        .body()
        .getElementsByTag("img")
        .stream()
        .map(element -> element.attr("src"))
        .filter(StrUtil::isNotBlank)
        .peek(key -> {
          final var picture = new Picture();
          picture.setUrl(key);
          picture.setEvidences(Set.of(newEvidence(datasource)));
          picture.setCreator(datasource.getCreator());
          pictureRepository.save(picture);
        })
        .collect(Collectors.toSet());
  }

  private Vertex getVertex(String name, String type) {

    if (StrUtil.isBlank(name) || StrUtil.isBlank(type)) {
      log.warn("实体必填项有缺失：name={} type={}", name, type);
      return null;
    }

    return vertexRepository.findByNameAndType(name, type).orElse(null);
  }

  private @Nonnull Evidence newEvidence(@Nonnull Datasource datasource) {

    final var evidence = new Evidence();
    evidence.setContent(datasource.getTitle());
    evidence.setDatasource(datasource);
    return evidenceRepository.save(evidence);
  }
}
