package com.singhand.sr.graphservice.bizkeycloakmodel.validator;

import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakRoleService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

public class NotSuperAdminRoleIdValidator implements
    ConstraintValidator<NotSuperAdminRoleId, Set<String>> {

  private final String roleId;

  @Autowired
  public NotSuperAdminRoleIdValidator(KeycloakRoleService keycloakRoleService) {

    roleId = keycloakRoleService.getRole("超级管理员").getId();
  }

  @Override
  public void initialize(NotSuperAdminRoleId constraintAnnotation) {

    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(Set<String> value, ConstraintValidatorContext context) {

    return !value.contains(roleId);
  }
}
