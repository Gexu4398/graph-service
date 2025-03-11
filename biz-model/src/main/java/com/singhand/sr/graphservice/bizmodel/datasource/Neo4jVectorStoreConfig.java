package com.singhand.sr.graphservice.bizmodel.datasource;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.driver.Driver;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore.Neo4jDistanceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.neo4j")
@Setter
@Getter
public class Neo4jVectorStoreConfig {

  private String databaseName;

  private Neo4jDistanceType distanceType;

  private int embeddingDimension;

  private String indexName;

  private boolean initializeSchema;

  private String label;

  @Value("${spring.ai.openai.api-key}")
  private String apiKey;

  @Value("${spring.ai.openai.base-url}")
  private String baseUrl;

  @Bean
  public Neo4jVectorStore neo4jVectorStore(Driver neo4jDriver, EmbeddingModel embeddingModel) {

    return Neo4jVectorStore.builder(neo4jDriver, embeddingModel)
        .databaseName(databaseName)
        .distanceType(distanceType)
        .embeddingDimension(embeddingDimension)
        .label(label)
        .indexName(indexName)
        .initializeSchema(initializeSchema)
        .batchingStrategy(new TokenCountBatchingStrategy())
        .build();
  }

  @Bean
  public EmbeddingModel embeddingModel() {

    return new OpenAiEmbeddingModel(OpenAiApi.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .build());
  }
}
