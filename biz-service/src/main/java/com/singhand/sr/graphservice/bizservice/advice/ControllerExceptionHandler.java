package com.singhand.sr.graphservice.bizservice.advice;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ControllerExceptionHandler {

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({ValidationException.class, ConstraintViolationException.class})
  public void exceptionHandler() {

  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(value = EmptyResultDataAccessException.class)
  public void emptyResultDataAccessExceptionHandler() {

  }
}