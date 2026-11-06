package com.isldevs.portfolio.contact.dto;

import com.isldevs.portfolio.contact.entity.Status;
import java.time.OffsetDateTime;
import java.util.UUID;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MessageResponse(
    UUID id,
    String name,
    String email,
    String subject,
    String message,
    Status status,
    long version,
    OffsetDateTime createdAt) {}
