package com.singhand.sr.graphservice.bizkeycloakmodel.validator;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;

public class NotContainsSuperAdminUserIdValidator implements
    ConstraintValidator<NotContainsSuperAdminUserId, Collection<String>> {

  private final String userId;

  @Autowired
  public NotContainsSuperAdminUserIdValidator(KeycloakService keycloakService) {

    userId = keycloakService.getUserResource("admin").toRepresentation().getId();
  }

  @Override
  public void initialize(NotContainsSuperAdminUserId constraintAnnotation) {

    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(Collection<String> value, ConstraintValidatorContext context) {

    return value.stream().noneMatch(it -> it.equalsIgnoreCase(userId));
  }
}
