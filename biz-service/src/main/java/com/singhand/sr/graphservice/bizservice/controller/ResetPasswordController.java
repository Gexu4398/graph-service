package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakUserService;
import com.singhand.sr.graphservice.bizkeycloakmodel.validator.NotSuperAdminUsername;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.bizlogger.BizLogger;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver.GetNameByUserNameInPathResolver;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver.Resolve;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户重置")
@RestController
@RequestMapping("reset-password")
@Validated
public class ResetPasswordController {

  private final KeycloakUserService keycloakUserService;

  @Autowired
  public ResetPasswordController(KeycloakUserService keycloakUserService) {

    this.keycloakUserService = keycloakUserService;
  }

  @BizLogger(
      type = "登录",
      module = @Resolve("'登录'"),
      contentFormat = "用户【%s】申请重置密码",
      contentFormatArguments = @Resolve(value = "request.path.username"),
      targetId = @Resolve(value = "request.path.username", resolver = GetNameByUserNameInPathResolver.class),
      targetName = @Resolve(value = "request.path.username"),
      targetType = @Resolve("'用户'"),
      isLogin = false
  )
  @PostMapping("{username}")
  @Operation(summary = "用户重置")
  @PreAuthorize("isAnonymous()")
  public void reset(@NotSuperAdminUsername @PathVariable("username") String username) {

    keycloakUserService.resetUserCredentialByAnonymous(username);
  }
}
