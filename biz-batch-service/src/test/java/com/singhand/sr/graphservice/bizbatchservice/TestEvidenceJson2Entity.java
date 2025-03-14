package com.singhand.sr.graphservice.bizbatchservice;

import cn.hutool.core.io.resource.ResourceUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.singhand.sr.graphservice.bizbatchservice.model.response.AiEvidenceExtractorResponse;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestEvidenceJson2Entity {

  @Test
  @SneakyThrows
  void testJson2Entity() {

    @Cleanup final var inputStream = ResourceUtil.getStream("evidence.json");

    final var objectMapper = new ObjectMapper();
    final var readValue = objectMapper
        .readValue(inputStream, AiEvidenceExtractorResponse.class);

    readValue.getLabels().forEach(it ->
        it.forEach((key, value) ->
            value.forEach(label -> {
              final var labelText = label.getText();
              final var relations = label.getRelations().entrySet()
                  .stream()
                  .map(entry -> {
                    final var stringBuilder = new StringBuilder();
                    for (final var entryValue : entry.getValue()) {
                      stringBuilder.append(entryValue.getText());
                    }
                    return String.format("%s:%s", entry.getKey(), stringBuilder);
                  }).toList();
              final var content = String.format("事件类型：%s, 事件描述：%s, 属性：%s",
                  label.getEventType(), labelText, relations);
              log.info("content: {}", content);
              Assertions.assertEquals("其他", label.getEventType());
            })));
  }
}
