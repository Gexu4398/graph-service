package com.singhand.sr.graphservice.bizbatchservice.tasklet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizbatchservice.converter.MsWordConverter;
import com.singhand.sr.graphservice.bizbatchservice.converter.PdfConverter;
import com.singhand.sr.graphservice.bizbatchservice.converter.TxtConverter;
import com.singhand.sr.graphservice.bizbatchservice.converter.picture.S3PictureManager;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.DatasourceContent;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Picture;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.EvidenceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PictureRepository;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
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

  private final MsWordConverter msWordConverter;

  private final PdfConverter pdfConverter;

  private final PlatformTransactionManager bizTransactionManager;

  private final EntityManager bizEntityManager;

  private final PictureRepository pictureRepository;

  private final EvidenceRepository evidenceRepository;

  @Autowired
  public ImportDatasourceTasklet(MinioClient minioClient,
      @Value("${minio.bucket}") String bucket,
      @Value("#{jobParameters['id']}") Long id,
      DatasourceRepository datasourceRepository,
      S3PictureManager s3PictureManager,
      @Qualifier("bizTransactionManager") PlatformTransactionManager bizTransactionManager,
      @Qualifier("bizEntityManager") EntityManager bizEntityManager,
      PictureRepository pictureRepository, EvidenceRepository evidenceRepository) {

    this.minioClient = minioClient;
    this.bucket = bucket;
    this.datasourceRepository = datasourceRepository;
    this.id = id;
    this.msWordConverter = new MsWordConverter(s3PictureManager);
    this.pdfConverter = new PdfConverter(s3PictureManager);
    this.bizTransactionManager = bizTransactionManager;
    this.bizEntityManager = bizEntityManager;
    this.pictureRepository = pictureRepository;
    this.evidenceRepository = evidenceRepository;
  }

  @Override
  public RepeatStatus execute(@NonNull StepContribution stepContribution,
      @NonNull ChunkContext chunkContext) throws Exception {

    final var stepContext = stepContribution.getStepExecution();
    final var url = stepContext.getJobParameters().getString("url", "");
    final var datasource = datasourceRepository.findById(id).orElseThrow();
    final var object = StrUtil.subAfter(StrUtil.isNotBlank(url) ? url : datasource.getUrl(), bucket,
        true);

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
    final var datasourceContent = new DatasourceContent();
    final List<String> paragraphs = switch (extName) {
      case "txt" -> {
        final var str = FileUtil.readString(tempFilename, StandardCharsets.UTF_8);
        datasourceContent.setHtml(TxtConverter.str2html(str));
        datasourceContent.setText(str);
        yield StrUtil.split(str, "\n");
      }
      case "docx" -> {
        datasourceContent.setHtml(msWordConverter.docx2html(tempFilename));
        datasourceContent.setText(msWordConverter.docx2txt(tempFilename));
        yield msWordConverter.docx2lines(tempFilename);
      }
      case "doc" -> {
        datasourceContent.setHtml(msWordConverter.doc2html(tempFilename));
        datasourceContent.setText(msWordConverter.doc2txt(tempFilename));
        yield msWordConverter.doc2lines(tempFilename);
      }
      case "pdf" -> {
        datasourceContent.setHtml(pdfConverter.pdf2html(tempFilename));
        datasourceContent.setText(pdfConverter.pdf2txt(tempFilename));
        yield pdfConverter.pdf2lines(tempFilename);
      }
      default -> {
        log.warn("不支持的文件类型：{}", extName);
        yield List.of();
      }
    };

    datasource.addContent(datasourceContent);

    // 此处要开辟新事务，否则会合并到最外层事务再提交，进而导致导入过程看不到数据源的问题。
    final var transaction = new TransactionTemplate(bizTransactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.execute(status -> {
      datasourceRepository.save(datasource);
      return savePictures(datasource, datasourceContent);
    });
    bizEntityManager.flush();

    return RepeatStatus.FINISHED;
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

  private Evidence newEvidence(@Nonnull Datasource datasource) {

    final var evidence = new Evidence();
    evidence.setContent(datasource.getTitle());
    evidence.setDatasource(datasource);
    return evidenceRepository.save(evidence);
  }
}
