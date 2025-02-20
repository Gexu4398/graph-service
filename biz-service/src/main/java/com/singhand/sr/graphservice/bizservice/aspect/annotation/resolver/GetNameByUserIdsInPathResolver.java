package com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetNameByUserIdsInPathResolver implements Resolver {

  private final KeycloakService keycloakService;

  @Autowired
  public GetNameByUserIdsInPathResolver(KeycloakService keycloakService) {

    this.keycloakService = keycloakService;
  }

  @Override
  public Object getProperty(ProceedingJoinPoint joinPoint, Object proceed, String beanPath) {

    final var value = DefaultResolver.getValueFromRequestPath(beanPath);
    var name = "";
    if (String.valueOf(value).contains(",")) {
      final var stringBuilder = new StringBuilder();
      final var split = StringUtils.split(String.valueOf(value), ",");
      for (var id : split) {
        final var userResource = keycloakService.getUserResourceById(id);
        stringBuilder.append(userResource.toRepresentation().getUsername()).append(",");
      }
      name = stringBuilder.substring(0, stringBuilder.toString().lastIndexOf(","));
    } else {
      final var userResource = keycloakService.getUserResourceById(String.valueOf(value));
      name = userResource.toRepresentation().getUsername();
    }
    return name;
  }
}
