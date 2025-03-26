package com.singhand.sr.graphservice.bizgraph.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

  @Bean
  public EmbeddingStore<TextSegment> neo4jVectorStore(Driver neo4jDriver) {

    return Neo4jEmbeddingStore.builder()
        .driver(neo4jDriver)
        .dimension(768)
        .databaseName("neo4j")
        .indexName("graph-embedding-index")
        .label("VertexDocument")
        .build();
  }

  @Bean
  public EmbeddingModel embeddingModel(
      @Value("${langchain4j.open-ai.embedding-model.api-key}") String apiKey,
      @Value("${langchain4j.open-ai.embedding-model.base-url}") String baseUrl,
      @Value("${langchain4j.open-ai.embedding-model.model-name}") String modelName) {

    final var build = new OpenAiEmbeddingModelBuilder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .modelName(modelName);
    return new OpenAiEmbeddingModel(build);
  }
}
