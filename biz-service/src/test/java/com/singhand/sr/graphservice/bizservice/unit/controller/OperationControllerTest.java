package com.singhand.sr.graphservice.bizservice.unit.controller;

import cn.hutool.json.JSONUtil;
import com.singhand.sr.graphservice.bizgraph.model.request.NewOntologyRequest;
import com.singhand.sr.graphservice.testenvironments.UnitTestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WithMockUser(username = "admin")
public class OperationControllerTest extends UnitTestEnvironment {

  @Test
  @SneakyThrows
  void testNewGetOntology() {

    final var request = new NewOntologyRequest();
    request.setName(faker.name().fullName());

    mockMvc.perform(post("/ontology")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(request)))
        .andExpect(status().isOk());
  }
}
