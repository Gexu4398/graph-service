package com.singhand.sr.graphservice.bizkeycloakmodel.validator;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

public class NotSuperAdminUserIdValidator implements
    ConstraintValidator<NotSuperAdminUserId, String> {

  private final String userId;

  @Autowired
  public NotSuperAdminUserIdValidator(KeycloakService keycloakService) {

    userId = keycloakService.getUserResource("admin").toRepresentation().getId();
  }

  @Override
  public void initialize(NotSuperAdminUserId constraintAnnotation) {

    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    return !userId.equalsIgnoreCase(value);
  }
}
