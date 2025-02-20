package com.singhand.sr.graphservice.bizservice.aspect.annotation.bizlogger;

import com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver.Resolve;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author singhand
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(BizLoggers.class)
public @interface BizLogger {

  Resolve module();

  String type();

  String contentFormat();

  Resolve[] contentFormatArguments() default {};

  Resolve targetId();

  Resolve targetName();

  Resolve targetType();

  boolean isLogin() default true;
}
