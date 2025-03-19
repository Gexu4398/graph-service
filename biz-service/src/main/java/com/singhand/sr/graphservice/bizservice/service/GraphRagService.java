package com.singhand.sr.graphservice.bizservice.service;

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

  @Autowired
  public GraphRagService(ChatClient.Builder chatClientBuilder, Neo4jVectorStore vectorStore) {

    this.chatClient = chatClientBuilder.build();
    this.vectorStore = vectorStore;
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
        .system("""
            你是一个专门处理图数据库信息的助手。基于以下上下文信息回答用户问题。
            
            回答规则：
            1. 如果是查询某个节点的属性或基本信息，直接提供相关信息
            2. 如果上下文中没有直接相关信息，尝试通过已知关系进行推理
            3. 在处理关系查询时：
               - 理解关系的反向含义，例如父子、兄弟、夫妻等关系都有对应的反向关系
               - 即使上下文只包含单向关系，也要基于常识推断出对应的反向关系进行回答
            4. 如果确实无法回答，请明确说明"根据已有信息无法回答该问题"
            5. 回答要简洁清晰，突出重点
            6. 对于关键实体，请在回答中包含其类型，便于用户进一步查询
            7. 当查询包含属性条件（如年龄、名称等）时：
               - 优先在上下文中查找匹配的节点属性
               - 若无精确匹配，返回近似值并标注匹配度
               - 对数值型属性支持范围查询逻辑
            """)
        .user(ragRequest.getQuestion())
        .call()
        .content();
  }
}
