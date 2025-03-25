package com.singhand.sr.graphservice.bizgraph.config;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SystemPromptConfig {

  private final ResourceLoader resourceLoader;

  private final String promptPath;

  @Getter
  private String systemPrompt;

  public SystemPromptConfig(
      ResourceLoader resourceLoader,
      @Value("${rag.system-prompt-path:classpath:prompts/system-prompt.txt}") String promptPath) {

    this.resourceLoader = resourceLoader;
    this.promptPath = promptPath;
  }

  @PostConstruct
  public void init() {

    loadSystemPrompt();
  }

  private void loadSystemPrompt() {

    try {
      Resource resource = resourceLoader.getResource(promptPath);
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
        this.systemPrompt = reader.lines().collect(Collectors.joining("\n"));
        log.info("成功加载系统提示词配置，长度：{} 字符", this.systemPrompt.length());
      }
    } catch (IOException e) {
      log.error("无法读取系统提示词配置文件: {}", promptPath, e);
      // 提供一个默认简单提示词，避免系统崩溃
      this.systemPrompt = "你是一个专业的图数据库知识助手。根据提供的上下文信息回答用户的查询。";
    }
  }
}
