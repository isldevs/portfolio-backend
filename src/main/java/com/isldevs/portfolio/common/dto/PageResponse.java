package com.isldevs.portfolio.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PageResponse<T>(
    List<T> content, long totalElements, int totalPages, int number, int size) {
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize());
  }
}
