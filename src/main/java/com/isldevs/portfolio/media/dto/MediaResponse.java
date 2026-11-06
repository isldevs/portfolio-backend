package com.isldevs.portfolio.media.dto;

import com.isldevs.portfolio.media.entity.Visibility;
import java.time.OffsetDateTime;
import java.util.UUID;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MediaResponse(
    UUID id,
    String url,
    String originalName,
    String contentType,
    long size,
    Visibility visibility,
    OffsetDateTime createdAt) {}
