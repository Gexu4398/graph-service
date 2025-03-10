package com.singhand.sr.graphservice.bizgraph.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NullOrLongIdValidator implements ConstraintValidator<NullOrLongId, Long> {

  @Override
  public void initialize(NullOrLongId constraintAnnotation) {

    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(Long value, ConstraintValidatorContext context) {

    return value == null || value > 0L;
  }
}
