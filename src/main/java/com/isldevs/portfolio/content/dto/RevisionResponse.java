package com.isldevs.portfolio.content.dto;

import java.time.Instant;
import java.util.UUID;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RevisionResponse(
    UUID id, Instant createdAt, String actor, ContentResponse snapshot) {}
