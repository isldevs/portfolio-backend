package com.isldevs.portfolio.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
    UUID id, String action, String resource, String resourceId, String actor, Instant createdAt) {}
