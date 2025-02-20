package com.singhand.sr.graphservice.bizservice.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

@Configuration
public class ValidatorConfig {

  @Bean
  public Validator validator(final AutowireCapableBeanFactory autowireCapableBeanFactory) {

    final var validatorFactory = Validation.byProvider(HibernateValidator.class)
        .configure()
        .constraintValidatorFactory(
            new SpringConstraintValidatorFactory(autowireCapableBeanFactory))
        .buildValidatorFactory();

    return validatorFactory.getValidator();
  }
}
