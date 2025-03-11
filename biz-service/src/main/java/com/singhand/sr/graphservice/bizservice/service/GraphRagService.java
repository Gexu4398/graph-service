package com.singhand.sr.graphservice.bizservice.service;

import cn.hutool.core.collection.CollUtil;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphRagService {

  private final ChatClient chatClient;

  private final Neo4jVectorStore vectorStore;

  @Autowired
  public GraphRagService(ChatClient.Builder chatClientBuilder, Neo4jVectorStore vectorStore) {

    this.chatClient = chatClientBuilder.build();
    this.vectorStore = vectorStore;
  }

  public String query(String userQuery) {

    final var searchRequest = SearchRequest.builder()
        .query(userQuery)
        .topK(3)
        .build();

    var relevantDocs = vectorStore.similaritySearch(searchRequest);

    if (CollUtil.isEmpty(relevantDocs)) {
      relevantDocs = new ArrayList<>();
    }

    final var contextContent = relevantDocs.stream()
        .map(doc -> String.format("ID: %s\n%s", doc.getId(), doc.getFormattedContent()))
        .collect(Collectors.joining("\n\n"));

    final var promptText = String.format(
        "基于以下上下文信息回答问题。如果上下文中没有相关信息，请说明无法回答。\n\n上下文:\n%s\n\n问题: %s",
        contextContent, userQuery);

    final var prompt = new Prompt(new UserMessage(promptText));

    return chatClient.prompt(prompt).call().content();
  }
}
