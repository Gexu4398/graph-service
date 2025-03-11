package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizservice.service.GraphRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("rag")
@Tag(name = "图谱RAG管理")
@Validated
public class GraphRagController {

  private final GraphRagService graphRagService;

  private final ChatModel chatModel;

  @Autowired
  public GraphRagController(GraphRagService graphRagService, ChatModel chatModel) {

    this.graphRagService = graphRagService;
    this.chatModel = chatModel;
  }

  @Operation(summary = "通过RAG查询")
  @GetMapping
  public String queryRag(@RequestParam String query) {

    return graphRagService.query(query);
  }

  @Operation(summary = "大模型对话")
  @GetMapping("/conversation")
  public String conversation(@RequestParam("message") String message) {

    return chatModel.call(message);
  }
}
