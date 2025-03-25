package com.singhand.sr.graphservice.bizservice.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.service.VertexService;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Datasource_;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Edge;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Evidence;
import com.singhand.sr.graphservice.bizmodel.model.jpa.PropertyValue;
import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceContentRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.DatasourceRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.PictureRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.VertexRepository;
import com.singhand.sr.graphservice.bizservice.model.response.DatasourceResponse;
import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DatasourceService {

  private final DatasourceRepository datasourceRepository;

  private final VertexService vertexService;

  private final PictureRepository pictureRepository;

  private final MinioService minioService;

  private final DatasourceContentRepository datasourceContentRepository;

  private final VertexRepository vertexRepository;

  @Autowired
  public DatasourceService(DatasourceRepository datasourceRepository, VertexService vertexService,
      PictureRepository pictureRepository, MinioService minioService,
      DatasourceContentRepository datasourceContentRepository, VertexRepository vertexRepository) {

    this.datasourceRepository = datasourceRepository;
    this.vertexService = vertexService;
    this.pictureRepository = pictureRepository;
    this.minioService = minioService;
    this.datasourceContentRepository = datasourceContentRepository;
    this.vertexRepository = vertexRepository;
  }

  public Optional<Datasource> getDatasource(Long id) {

    return datasourceRepository.findById(id);
  }

  public Datasource newDatasource(Datasource datasource) {

    return datasourceRepository.save(datasource);
  }

  public Datasource attachVertex(@Nonnull Datasource datasource, @Nonnull Vertex vertex) {

    vertex.attachDatasource(datasource);
    return datasource;
  }

  public Datasource detachVertex(@Nonnull Datasource datasource, @Nonnull Vertex vertex) {

    datasource.detachVertex(vertex);

    final var emptyPropertyValues = new HashMap<String, Set<PropertyValue>>();
    vertex.getProperties()
        .forEach((key, value) -> {
          final var emptyValues = new HashSet<PropertyValue>();
          value.getValues().forEach(propertyValue ->
              getValue(datasource, propertyValue).ifPresent(emptyValues::add));
          if (CollUtil.isNotEmpty(emptyValues)) {
            emptyPropertyValues.put(key, emptyValues);
          }
        });

    emptyPropertyValues.forEach((key, value) ->
        value.forEach(propertyValue ->
            vertexService.deletePropertyValue(vertex, key, propertyValue.getMd5(), "md5")));

    vertex.getEdges().forEach(Edge::clearEvidences);

    return datasource;
  }

  private Optional<PropertyValue> getValue(@Nonnull Datasource datasource,
      @Nonnull PropertyValue propertyValue) {

    propertyValue.getEvidences()
        .stream()
        .filter(it -> it.getDatasource().getID().equals(datasource.getID()))
        .findFirst()
        .ifPresent(Evidence::detachAll);

    if (CollUtil.isEmpty(propertyValue.getEvidences())) {
      return Optional.of(propertyValue);
    }
    return Optional.empty();
  }

  public DatasourceResponse updateImageElements(@Nonnull DatasourceResponse datasourceResponse) {

    final var datasourceContent = datasourceContentRepository
        .findById(datasourceResponse.getID())
        .orElse(null);

    if (null != datasourceContent) {
      datasourceResponse.setText(datasourceContent.getText());

      final var html = datasourceContent.getHtml();
      if (StrUtil.isNotBlank(html)) {
        final var document = Jsoup.parse(html);
        final var images = document.getElementsByTag("img");
        for (final var image : images) {
          final var src = image.attr("src");
          if (StrUtil.isBlank(src) || StrUtil.startWith(src, "data")) {
            continue;
          }
          pictureRepository.findByUrl(src).forEach(picture -> {
            final var object = minioService.getObjectFromKey(picture.getUrl());
            if (StrUtil.isNotBlank(object)) {
              final var preSignedUrl = minioService.getPreSignedURL(object);
              image.attr("src", preSignedUrl);
              image.attr("data-id", picture.getID().toString());
            }
          });
        }
        datasourceResponse.setHtml(document.html());
      }
    }

    return datasourceResponse;
  }

  public Page<Datasource> getDataSources(String keyword, Pageable pageable) {

    return datasourceRepository.findAll(Specification.where(titleLike(keyword)), pageable);
  }

  private static @Nonnull Specification<Datasource> titleLike(String keyword) {

    return (root, query, criteriaBuilder) -> {
      Objects.requireNonNull(query).distinct(true);
      if (StrUtil.isBlank(keyword)) {
        return criteriaBuilder.and();
      }

      return criteriaBuilder.like(root.get(Datasource_.TITLE), "%" + keyword + "%");
    };
  }

  public void deleteDatasource(Long id) {

    datasourceRepository.findById(id).ifPresentOrElse(datasource -> {
      datasource.clearEvidences();
      detachAllVertex(datasource);
      datasourceContentRepository.findById(datasource.getID())
          .ifPresent(datasourceContentRepository::delete);
      datasourceRepository.delete(datasource);
    }, () -> {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在");
    });
  }

  private void detachAllVertex(@Nonnull Datasource datasource) {

    final var vertices = new HashSet<>(
        vertexRepository.findAll(Specification.where(datasourceIdIs(datasource.getID())))
    );

    final var emptyPropertyValues = new HashMap<String, Set<PropertyValue>>();
    vertices.forEach(vertex -> {
      vertex.getProperties()
          .forEach((key, value) -> {
            final var emptyValues = new HashSet<PropertyValue>();
            value.getValues().forEach(propertyValue ->
                getValue(datasource, propertyValue).ifPresent(emptyValues::add));
            if (CollUtil.isNotEmpty(emptyValues)) {
              emptyPropertyValues.put(key, emptyValues);
            }
          });
      emptyPropertyValues.forEach((k, v) -> v.forEach(
          value -> vertexService.deletePropertyValue(vertex, k, value.getMd5(), "md5")));
      vertex.getEdges().forEach(Edge::clearEvidences);
      vertex.getDatasources().remove(datasource);
    });

    datasource.getVertices().clear();
  }

  private static @Nonnull Specification<Vertex> datasourceIdIs(Long id) {

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Datasource_._ID), id);
  }
}
