package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizgraph.model.GraphItem;
import com.singhand.sr.graphservice.bizgraph.model.request.RagRequest;
import com.singhand.sr.graphservice.bizgraph.service.impl.GraphRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("rag")
@Tag(name = "图谱RAG管理")
@Validated
public class GraphRagController {

  private final GraphRagService graphRagService;

  @Autowired
  public GraphRagController(GraphRagService graphRagService) {

    this.graphRagService = graphRagService;
  }

  /**
   * 通过RAG查询
   *
   * @param ragRequest 查询语句
   * @return 查询结果
   */
  @Operation(summary = "通过RAG查询")
  @PostMapping
  public String queryRag(@Valid @RequestBody RagRequest ragRequest) {

    return graphRagService.query(ragRequest);
  }

  @Operation(summary = "根据文本抽取结构化数据（调试用）")
  @PostMapping("extract")
  public GraphItem extract(@Valid @RequestBody RagRequest ragRequest) {

    return graphRagService.extractGraph(ragRequest);
  }
}
