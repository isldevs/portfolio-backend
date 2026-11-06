package com.isldevs.portfolio.common.exception;

import org.springframework.http.*;
import org.springframework.web.ErrorResponseException;

public class RateLimitException extends ErrorResponseException {
  public RateLimitException() {
    super(
        HttpStatus.TOO_MANY_REQUESTS,
        ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Please try again later"),
        null);
    getHeaders().set("Retry-After", "3600");
  }
}
