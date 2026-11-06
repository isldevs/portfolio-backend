package com.isldevs.portfolio.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ContentRequest(
    UUID id,
    String kind,
    @NotBlank @Size(max = 240) String title,
    @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 180) String slug,
    @Size(max = 4000) String summary,
    Map<String, Object> body,
    Map<String, Object> data,
    @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED") String status,
    Boolean featured,
    Integer displayOrder,
    Long version,
    Instant createdAt,
    Instant updatedAt,
    Instant publishedAt) {}
