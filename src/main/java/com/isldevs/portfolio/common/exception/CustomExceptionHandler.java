package com.isldevs.portfolio.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class CustomExceptionHandler {
  @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
  public org.springframework.http.ResponseEntity<ProblemDetail> errorResponse(
      org.springframework.web.ErrorResponseException e) {
    return new org.springframework.http.ResponseEntity<>(
        e.getBody(), e.getHeaders(), e.getStatusCode());
  }

  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ProblemDetail responseStatus(org.springframework.web.server.ResponseStatusException e) {
    return ProblemDetail.forStatusAndDetail(
        e.getStatusCode(), e.getReason() == null ? "Request failed." : e.getReason());
  }

  @ExceptionHandler(ApiException.class)
  public ProblemDetail api(ApiException e) {
    return ProblemDetail.forStatusAndDetail(e.status, e.getMessage());
  }

  @ExceptionHandler({
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

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail duplicate(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "A record with this slug already exists, or this record is still referenced.");
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ProblemDetail stale(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "This record has changed. Reload it before saving.");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ProblemDetail size(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYLOAD_TOO_LARGE, "Upload exceeds the 10 MB limit.");
  }
}
