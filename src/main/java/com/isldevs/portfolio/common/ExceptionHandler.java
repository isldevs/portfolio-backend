package com.isldevs.portfolio.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ExceptionHandler {
  @org.springframework.web.bind.annotation.ExceptionHandler(
      org.springframework.web.server.ResponseStatusException.class)
  public ProblemDetail responseStatus(org.springframework.web.server.ResponseStatusException e) {
    return ProblemDetail.forStatusAndDetail(
        e.getStatusCode(), e.getReason() == null ? "Request failed." : e.getReason());
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(ApiException.class)
  public ProblemDetail api(ApiException e) {
    return ProblemDetail.forStatusAndDetail(e.status, e.getMessage());
  }

  @org.springframework.web.bind.annotation.ExceptionHandler({
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    IllegalArgumentException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  public ProblemDetail validation(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Invalid request. Check required fields and their lengths.");
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail duplicate(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "A record with this slug already exists, or this record is still referenced.");
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      ObjectOptimisticLockingFailureException.class)
  public ProblemDetail stale(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "This record has changed. Reload it before saving.");
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(MaxUploadSizeExceededException.class)
  public ProblemDetail size(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYLOAD_TOO_LARGE, "Upload exceeds the 10 MB limit.");
  }
}
