package com.singhand.sr.graphservice.testenvironments.mock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MockDataSources.class)
public @interface MockDataSource {

  String title();

  String sourceType() default "sourceType";

  String contentType() default "contentType";

  String source() default "source";
}
