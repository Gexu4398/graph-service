package com.singhand.sr.graphservice.bizgraph.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

  @Bean
  public ChatLanguageModel chatLanguageModel(
      @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
      @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl,
      @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName) {

    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .modelName(modelName)
        .build();
  }
}
