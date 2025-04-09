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

  private final String extractGraphPromptPath;

  @Getter
  private String systemPrompt;

  @Getter
  private String extractGraphPrompt;

  public SystemPromptConfig(
      ResourceLoader resourceLoader,
      @Value("classpath:prompts/system-prompt.txt") String promptPath,
      @Value("classpath:prompts/extract_graph.txt") String extractGraphPromptPath) {

    this.resourceLoader = resourceLoader;
    this.promptPath = promptPath;
    this.extractGraphPromptPath = extractGraphPromptPath;
  }

  @PostConstruct
  public void init() throws IOException {

    loadSystemPrompt();
    loadExtractGraphPrompt();
  }

  private void loadSystemPrompt() throws IOException {

    final var resource = resourceLoader.getResource(promptPath);
    try (final var reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      this.systemPrompt = reader.lines().collect(Collectors.joining("\n"));
      log.info("成功加载系统提示词配置，长度：{} 字符", this.systemPrompt.length());
    }
  }

  private void loadExtractGraphPrompt() throws IOException {

    final var resource = resourceLoader.getResource(extractGraphPromptPath);
    try (final var reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      this.extractGraphPrompt = reader.lines().collect(Collectors.joining("\n"));
      log.info("成功加载抽取提示词配置，长度：{} 字符", this.extractGraphPrompt.length());
    }
  }
}
