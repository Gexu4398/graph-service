package com.singhand.sr.graphservice.bizkeycloakmodel.validator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = NotSuperAdminRoleIdValidator.class)
@Documented
public @interface NotSuperAdminRoleId {

  String message() default "不能创建超级管理员角色的账号！";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
