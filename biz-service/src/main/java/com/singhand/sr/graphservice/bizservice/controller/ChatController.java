package com.singhand.sr.graphservice.bizservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("chat")
@Tag(name = "模型对话")
@Validated
public class ChatController {

  private final ChatModel chatModel;

  @Autowired
  public ChatController(ChatModel chatModel) {

    this.chatModel = chatModel;
  }

  @GetMapping("/conversation")
  public String conversation(@RequestParam("message") String message) {

    return chatModel.call(message);
  }
}
