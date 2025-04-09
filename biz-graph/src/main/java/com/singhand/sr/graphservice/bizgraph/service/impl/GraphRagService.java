package com.singhand.sr.graphservice.bizgraph.service.impl;

import cn.hutool.core.util.StrUtil;
import com.singhand.sr.graphservice.bizgraph.config.SystemPromptConfig;
import com.singhand.sr.graphservice.bizgraph.model.GraphItem;
import com.singhand.sr.graphservice.bizgraph.model.request.RagRequest;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyPropertyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.OntologyRepository;
import com.singhand.sr.graphservice.bizmodel.repository.jpa.RelationModelRepository;
import jakarta.annotation.Nonnull;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
public class GraphRagService {

  private final ChatClient chatClient;

  private final Neo4jVectorStore vectorStore;

  private final SystemPromptConfig systemPromptConfig;

  private final ChatModel chatModel;

  private final BeanOutputConverter<GraphItem> outputConverter = new BeanOutputConverter<>(
      new ParameterizedTypeReference<>() {

      });

  private final OntologyRepository ontologyRepository;

  private final RelationModelRepository relationModelRepository;

  private final OntologyPropertyRepository ontologyPropertyRepository;

  @Autowired
  public GraphRagService(ChatClient.Builder chatClientBuilder, Neo4jVectorStore vectorStore,
      SystemPromptConfig systemPromptConfig, ChatModel chatModel,
      OntologyRepository ontologyRepository, RelationModelRepository relationModelRepository,
      OntologyPropertyRepository ontologyPropertyRepository) {

    this.chatClient = chatClientBuilder.build();
    this.vectorStore = vectorStore;
    this.systemPromptConfig = systemPromptConfig;
    this.chatModel = chatModel;
    this.ontologyRepository = ontologyRepository;
    this.relationModelRepository = relationModelRepository;
    this.ontologyPropertyRepository = ontologyPropertyRepository;
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

  public GraphItem extractGraph(@Nonnull RagRequest ragRequest) {

    String format = outputConverter.getFormat();

    final var types = ontologyRepository.findAllNames();
    final var relationNames = relationModelRepository.findAllNames();
    final var propertyNames = ontologyPropertyRepository.findAllNames();

    // 提示词
    var graphPrompt = systemPromptConfig.getExtractGraphPrompt();
    graphPrompt = graphPrompt
        .replace("{input_text}", ragRequest.getQuestion())
        .replace("{vertex_type}", StrUtil.join(",", types))
        .replace("{relation_name}", StrUtil.join(",", relationNames))
        .replace("{property_name}", StrUtil.join(",", propertyNames));

    String templateString = """
        {prompt}
        {format}
        """;

    PromptTemplate template = new PromptTemplate(templateString,
        Map.of("prompt", graphPrompt, "format", format));

    Prompt prompt = template.create();

    Generation generation = chatModel.call(prompt).getResult();

    return outputConverter.convert(generation.getOutput().getText());
  }
}
