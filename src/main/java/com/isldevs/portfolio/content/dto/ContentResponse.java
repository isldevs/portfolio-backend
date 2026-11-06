package com.isldevs.portfolio.content.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ContentResponse(
    UUID id,
    String kind,
    String title,
    String slug,
    String summary,
    JsonNode body,
    JsonNode data,
    String status,
    Boolean featured,
    Integer displayOrder,
    Long version,
    Instant createdAt,
    Instant updatedAt,
    Instant publishedAt) {}
