package com.singhand.sr.graphservice.bizservice.service;

import java.util.ArrayList;
import java.util.Optional;
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
        .topK(5)
        .build();

    final var relevantDocs = Optional.ofNullable(vectorStore.similaritySearch(searchRequest))
        .orElseGet(ArrayList::new);

    final var contextContent = relevantDocs.stream()
        .map(doc -> {
          final var nodeType = doc.getMetadata().getOrDefault("type", "未知类型").toString();
          final var properties = doc.getMetadata().getOrDefault("properties", "").toString();
          final var relations = doc.getMetadata().getOrDefault("relations", "").toString();
          return String.format("ID: %s\n类型: %s\n属性：%s\n关系：%s\n%s",
              doc.getId(),
              nodeType,
              properties,
              relations,
              doc.getFormattedContent());
        })
        .collect(Collectors.joining("\n\n"));

    final var promptText = String.format(
        """
            你是一个知识图谱问答助手。基于以下上下文信息回答用户问题。
    
            回答规则：
            1. 如果是查询某个节点的属性或基本信息，直接提供相关信息
            2. 如果上下文中没有直接相关信息，尝试通过已知关系进行推理
            3. 在处理关系查询时：
               - 理解关系的反向含义，例如父子、兄弟、夫妻等关系都有对应的反向关系
               - 即使上下文只包含单向关系，也要基于常识推断出对应的反向关系进行回答
            4. 如果确实无法回答，请明确说明"根据已有信息无法回答该问题"
            5. 回答要简洁清晰，突出重点
            6. 对于关键实体，请在回答中包含其ID和类型，便于用户进一步查询
    
            上下文信息:
            %s
    
            用户问题: %s""",
        contextContent, userQuery);

    final var prompt = new Prompt(new UserMessage(promptText));

    return chatClient.prompt(prompt).call().content();
  }
}
