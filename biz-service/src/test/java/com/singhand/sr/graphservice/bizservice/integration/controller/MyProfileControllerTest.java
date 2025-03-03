package com.singhand.sr.graphservice.bizservice.integration.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.hutool.json.JSONUtil;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.User;
import com.singhand.sr.graphservice.testenvironments.KeycloakIntegrationTestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

@Slf4j
@WithMockUser(username = "admin", authorities = {"user:crud"})
public class MyProfileControllerTest extends KeycloakIntegrationTestEnvironment {

  @Test
  @SneakyThrows
  void testUpdateProfile() {

    final var user = new User();
    user.setName("超级管理员");
    user.setPhoneNumber(faker.phoneNumber().phoneNumber());
    user.setPicture(faker.internet().image());

    mockMvc.perform(put("/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSONUtil.toJsonStr(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phoneNumber", equalTo(user.getPhoneNumber())))
        .andExpect(jsonPath("$.picture", equalTo(user.getPicture())));
  }
}
