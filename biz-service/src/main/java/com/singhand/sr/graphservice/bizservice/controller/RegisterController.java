package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.User;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.RegisterUserRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakUserService;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.bizlogger.BizLogger;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver.Resolve;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "新用户注册")
@RestController
@RequestMapping("register")
public class RegisterController {

  private final KeycloakUserService keycloakUserService;

  @Autowired
  public RegisterController(KeycloakUserService keycloakUserService) {

    this.keycloakUserService = keycloakUserService;
  }

  @BizLogger(
      type = "登录",
      module = @Resolve("'登录'"),
      contentFormat = "用户【%s】申请注册账号",
      contentFormatArguments = @Resolve(value = "request.body.username"),
      targetId = @Resolve(value = "''"),
      targetName = @Resolve(value = "request.body.username"),
      targetType = @Resolve("'用户'"),
      isLogin = false
  )
  @PostMapping
  @Operation(summary = "新用户注册")
  @PreAuthorize("isAnonymous()")
  public User register(@RequestBody RegisterUserRequest request) {

    if (request.getUsername().startsWith("reserved_")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请务使用 reserved_ 开头命名！");
    }
    return keycloakUserService.registerUser(request);
  }
}
