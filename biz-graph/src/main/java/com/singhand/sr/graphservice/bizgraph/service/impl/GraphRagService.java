package com.singhand.sr.graphservice.bizgraph.service.impl;

import com.singhand.sr.graphservice.bizgraph.config.SystemPromptConfig;
import com.singhand.sr.graphservice.bizgraph.model.request.RagRequest;
import com.singhand.sr.graphservice.bizgraph.service.Assistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphRagService {

  private final EmbeddingModel embeddingModel;

  private final ChatLanguageModel chatLanguageModel;

  private final EmbeddingStore<TextSegment> embeddingStore;

  private final SystemPromptConfig systemPromptConfig;

  @Autowired
  public GraphRagService(EmbeddingModel embeddingModel, ChatLanguageModel chatLanguageModel,
      EmbeddingStore<TextSegment> embeddingStore, SystemPromptConfig systemPromptConfig) {

    this.embeddingModel = embeddingModel;
    this.chatLanguageModel = chatLanguageModel;
    this.embeddingStore = embeddingStore;
    this.systemPromptConfig = systemPromptConfig;
  }

  public String query(@Nonnull RagRequest ragRequest) {

    final var contentRetriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(embeddingStore)
        .embeddingModel(embeddingModel)
        .maxResults(5)
        .dynamicMaxResults(query -> 5)
        .minScore(0.75)
        .dynamicMinScore(query -> 0.75)
        .build();

    final var queryRouter = new DefaultQueryRouter(contentRetriever);

    final var retrievalAugmentor = DefaultRetrievalAugmentor.builder()
        .queryRouter(queryRouter)
        .build();

    final var assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(chatLanguageModel)
        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
        .retrievalAugmentor(retrievalAugmentor)
        .systemMessageProvider(memoryId -> systemPromptConfig.getSystemPrompt())
        .build();

    return assistant.chat(ragRequest.getQuestion());
  }
}
