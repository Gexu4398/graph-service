package com.singhand.sr.graphservice.bizservice.service;

import com.singhand.sr.graphservice.bizservice.config.SystemPromptConfig;
import com.singhand.sr.graphservice.bizservice.model.request.RagRequest;
import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphRagService {

  private final ChatClient chatClient;

  private final Neo4jVectorStore vectorStore;

  private final SystemPromptConfig systemPromptConfig;

  @Autowired
  public GraphRagService(ChatClient.Builder chatClientBuilder, Neo4jVectorStore vectorStore,
      SystemPromptConfig systemPromptConfig) {

    this.chatClient = chatClientBuilder.build();
    this.vectorStore = vectorStore;
    this.systemPromptConfig = systemPromptConfig;
  }

  public String query(@Nonnull RagRequest ragRequest) {

    final var retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
        .queryTransformers(RewriteQueryTransformer.builder()
            .chatClientBuilder(chatClient.mutate())
            .build())
        .documentRetriever(VectorStoreDocumentRetriever.builder()
            .similarityThreshold(0.5)
            .vectorStore(vectorStore)
            .build())
        .build();

    return chatClient.prompt()
        .advisors(retrievalAugmentationAdvisor)
        .system(systemPromptConfig.getSystemPrompt())
        .user(ragRequest.getQuestion())
        .call()
        .content();
  }
}
