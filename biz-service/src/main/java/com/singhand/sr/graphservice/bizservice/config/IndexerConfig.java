package com.singhand.sr.graphservice.bizservice.config;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Vertex;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
@Slf4j
public class IndexerConfig {

  private final EntityManager entityManager;

  private static final int THREAD_NUMBER = Runtime.getRuntime().availableProcessors();

  @Autowired
  public IndexerConfig(@Qualifier("bizEntityManager") EntityManager entityManager) {

    this.entityManager = entityManager;
  }

  @SneakyThrows
  public void indexPersistedData() {

    final var searchSession = Search.session(entityManager);

    final var vertexIndexer = searchSession.massIndexer(Vertex.class)
        .threadsToLoadObjects(THREAD_NUMBER);
    final var vertexSchemaManager = searchSession.schemaManager(Vertex.class);
    vertexSchemaManager.createIfMissing();
    vertexIndexer.startAndWait();
  }
}
