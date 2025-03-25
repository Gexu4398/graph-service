package com.singhand.sr.graphservice.bizshell.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.singhand.sr.graphservice.bizshell.model.xts.EvidenceTriple_v3;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

      jsonNode.put("id", node.id());
      jsonNode.putPOJO("labels", node.labels());

      final var props = node.asMap();
      final var propertiesNode = objectMapper.createObjectNode();
      props.forEach(propertiesNode::putPOJO);
      jsonNode.set("properties", propertiesNode);
      jsonArray.add(jsonNode);
    }
    objectMapper.writerWithDefaultPrettyPrinter()
        .writeValue(Files.newBufferedWriter(Paths.get(outputDirectory)), jsonArray);

    session.close();
    log.info("导出成功，文件路径：{}", outputDirectory);
  }

  @SneakyThrows
  public void outputVertexToExcel(String type, String outputDirectory) {

    try (var session = driver.session(); XSSFWorkbook workbook = new XSSFWorkbook()) {
      final var sheet = workbook.createSheet("Vertices");

      final var headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("id");
      headerRow.createCell(1).setCellValue("name");
      headerRow.createCell(2).setCellValue("source");

      final var cypher = String.format("MATCH (n:%s) RETURN n", type);
      final var result = session.run(cypher);
      int rowNum = 1;

      while (result.hasNext()) {
        final var node = result.next().get("n").asNode();
        final var props = node.asMap();

        log.info("正在处理: {}", node.id());

        final var dataRow = sheet.createRow(rowNum++);

        dataRow.createCell(0).setCellValue(node.id());

        dataRow.createCell(1).setCellValue(
            props.getOrDefault("name", "").toString()
        );

        dataRow.createCell(2).setCellValue(
            props.getOrDefault("source", "").toString()
        );
      }

      final var outputFile = Paths.get(outputDirectory).toFile();
      if (!outputFile.getParentFile().exists()) {
        outputFile.getParentFile().mkdirs();
      }

      try (final var out = new FileOutputStream(outputFile)) {
        workbook.write(out);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    log.info("导出成功，文件路径：{}", outputDirectory);
  }

  @SneakyThrows
  public void fileVertexToJson(String inputDirectory) {

    final var kbFile = new File(inputDirectory);
    final var triples = new ArrayList<EvidenceTriple_v3>();
    try (final var lineIterator = FileUtils.lineIterator(kbFile, "UTF-8")) {
      while (lineIterator.hasNext()) {
        final var line = lineIterator.nextLine();
        // 解析JSON行
        try {
          final var objectMapper = new ObjectMapper();
          final var tripleData = objectMapper.readValue(line, EvidenceTriple_v3.class);
          // 处理每一行数据
          if (StrUtil.equals(tripleData.getKnowledgeType().toString(), "relation")) {
            triples.add(tripleData);
          }
        } catch (Exception e) {
          log.error("解析行失败: {}", e.getMessage());
          log.error("问题行: {}", line);
        }
      }
    } catch (IOException e) {
      log.error("读取文件异常: {}", e.getMessage());
    }

    log.info("解析完成，共 {} 条数据", triples.size());
    final var objectMapper = new ObjectMapper();

    objectMapper.writerWithDefaultPrettyPrinter()
        .writeValue(Files.newBufferedWriter(Path.of("./entities.json")), triples);
  }
}
