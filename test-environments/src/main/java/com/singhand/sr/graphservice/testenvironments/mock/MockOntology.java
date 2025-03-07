package com.singhand.sr.graphservice.testenvironments.mock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MockOntologies.class)
public @interface MockOntology {

  String name();

  MockOntologyProperty[] properties() default {};
}
