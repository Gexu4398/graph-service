package com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetNameByUserNameInPathResolver implements Resolver {

  private final KeycloakService keycloakService;

  @Autowired
  public GetNameByUserNameInPathResolver(KeycloakService keycloakService) {

    this.keycloakService = keycloakService;
  }

  @Override
  public Object getProperty(ProceedingJoinPoint joinPoint, Object proceed, String beanPath) {

    final var value = DefaultResolver.getValueFromRequestPath(beanPath);

    return keycloakService.getUserResource(String.valueOf(value)).toRepresentation().getId();
  }
}
