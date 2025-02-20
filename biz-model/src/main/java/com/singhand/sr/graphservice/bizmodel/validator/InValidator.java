package com.singhand.sr.graphservice.bizmodel.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InValidator implements ConstraintValidator<In, String> {

  private final Set<String> set = new HashSet<>();

  @Override
  public void initialize(In constraintAnnotation) {

    ConstraintValidator.super.initialize(constraintAnnotation);

    set.addAll(Arrays.asList(constraintAnnotation.set()));
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    return set.contains(value);
  }
}
