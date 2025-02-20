package com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakGroupService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetNameByGroupIdResolver implements Resolver {

  private final KeycloakGroupService keycloakGroupService;

  @Autowired
  public GetNameByGroupIdResolver(KeycloakGroupService keycloakGroupService) {

    this.keycloakGroupService = keycloakGroupService;
  }

  @Override
  public Object getProperty(ProceedingJoinPoint joinPoint, Object proceed, String beanPath)
      throws Exception {

    final var value = DefaultResolver.getValueFromRequestPath(beanPath);
    final var group = keycloakGroupService.getGroup(String.valueOf(value));
    return group.getName();
  }
}
