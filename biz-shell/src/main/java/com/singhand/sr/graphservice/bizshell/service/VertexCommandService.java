package com.singhand.sr.graphservice.bizshell.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VertexCommandService {

  private final Driver driver;

  @Autowired
  public VertexCommandService(Driver driver) {

    this.driver = driver;
  }

  @SneakyThrows
  public void outputVertex(String type, String outputDirectory) {

    final var session = driver.session();

    final var cypher = String.format("MATCH (n:%s) RETURN n", type);

    final var objectMapper = new ObjectMapper();
    final var jsonArray = objectMapper.createArrayNode();

    final var result = session.run(cypher);
    while (result.hasNext()) {
      final var node = result.next().get("n").asNode();

      log.info("正在处理: {}", node.get("id").asString());

      final var jsonNode = objectMapper.createObjectNode();

      jsonNode.put("id", node.elementId());
      jsonNode.putPOJO("labels", node.labels());

      final var props = node.asMap();
      final var propertiesNode = objectMapper.createObjectNode();
      props.forEach(propertiesNode::putPOJO);
      jsonNode.set("properties", propertiesNode);
      jsonArray.add(jsonNode);
    }
    objectMapper.writerWithDefaultPrettyPrinter()
        .writeValue(Files.newBufferedWriter(Paths.get(outputDirectory)), jsonArray);

    log.info("导出成功，文件路径：{}", outputDirectory);
  }
}
