package com.singhand.sr.graphservice.bizbatchservice.tasklet;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizbatchservice.importer.helper.ExtractHelper;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizgraph.service.impl.GraphRagService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.DatasourceContent;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Picture;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceContentRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EdgeRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EvidenceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PictureRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PropertyValueRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
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

  private final VertexRepository vertexRepository;

  private final VertexService vertexService;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  private final RelationModelRepository relationModelRepository;

  private final EdgeRepository edgeRepository;

  private final ExtractHelper extractHelper;

  private final DatasourceContentRepository datasourceContentRepository;

  private final PropertyValueRepository propertyValueRepository;

  private final GraphRagService graphRagService;

  @Autowired
  public ImportDatasourceTasklet(MinioClient minioClient,
      @Value("${minio.bucket}") String bucket,
      @Value("#{jobParameters['id']}") Long id,
      DatasourceRepository datasourceRepository,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager,
      @Qualifier("bizEntityManager") EntityManager bizEntityManager,
      PictureRepository pictureRepository, EvidenceRepository evidenceRepository,
      OntologyRepository ontologyRepository, VertexRepository vertexRepository,
      VertexService vertexService, OntologyPropertyRepository ontologyPropertyRepository,
      RelationModelRepository relationModelRepository, EdgeRepository edgeRepository,
      ExtractHelper extractHelper, DatasourceContentRepository datasourceContentRepository,
      PropertyValueRepository propertyValueRepository, GraphRagService graphRagService) {

    this.minioClient = minioClient;
    this.bucket = bucket;
    this.datasourceRepository = datasourceRepository;
    this.id = id;
    this.bizTransactionManager = bizTransactionManager;
    this.bizEntityManager = bizEntityManager;
    this.pictureRepository = pictureRepository;
    this.evidenceRepository = evidenceRepository;
    this.ontologyRepository = ontologyRepository;
    this.vertexRepository = vertexRepository;
    this.vertexService = vertexService;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
    this.relationModelRepository = relationModelRepository;
    this.edgeRepository = edgeRepository;
    this.extractHelper = extractHelper;
    this.datasourceContentRepository = datasourceContentRepository;
    this.propertyValueRepository = propertyValueRepository;
    this.graphRagService = graphRagService;
  }

  @Override
  public RepeatStatus execute(@NonNull StepContribution stepContribution,
      @NonNull ChunkContext chunkContext) {

    final var stepContext = stepContribution.getStepExecution();
    final var url = stepContext.getJobParameters().getString("url", "");
    final var datasource = datasourceRepository.findById(id).orElseThrow();

    try {
      final var object = StrUtil.subAfter(StrUtil.isNotBlank(url) ? url : datasource.getUrl(),
          bucket, true);

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
          .findById(datasource.getID())
          .orElse(new DatasourceContent());

      datasourceContent.setID(datasource.getID());

      log.info("开始提取数据源内容.......... 文件名={}", object);
      final var paragraphs = extractHelper.extractFile(tempFilename, datasourceContent);
      log.info("数据源内容提取完毕.......... 文件名={}", object);

      final var transaction = new TransactionTemplate(bizTransactionManager);
      transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      transaction.execute(status -> {
        datasourceRepository.save(datasource);
        datasourceContentRepository.save(datasourceContent);
        return savePictures(datasource, datasourceContent.getHtml());
      });

      textExtract(paragraphs, datasource);

      updateDatasourceStatus(datasource, Datasource.STATUS_SUCCESS);

      bizEntityManager.flush();

      log.info("数据源导入完毕.......... 文件名={}", object);

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

    if (CollUtil.isEmpty(paragraphs)) {
      return;
    }

    paragraphs.forEach(paragraph -> {
      // todo 给提示词，然后让大模型以结构化数据输出，提取事件
    });
  }

  @SneakyThrows
  private Set<String> savePictures(@Nonnull Datasource datasource, String html) {

    final var document = Jsoup.parse(html);

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

  private @Nonnull Evidence newEvidence(@Nonnull Datasource datasource) {

    final var evidence = new Evidence();
    evidence.setContent(datasource.getTitle());
    evidence.setDatasource(datasource);
    return evidenceRepository.save(evidence);
  }
}
