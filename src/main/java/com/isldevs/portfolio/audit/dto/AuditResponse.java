package com.isldevs.portfolio.audit.dto;

import java.time.Instant;
import java.util.UUID;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AuditResponse(
    UUID id, String action, String resource, String resourceId, String actor, Instant createdAt) {}
