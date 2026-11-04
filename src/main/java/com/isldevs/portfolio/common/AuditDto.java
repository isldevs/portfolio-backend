package com.isldevs.portfolio.common;

import java.time.Instant;
import java.util.UUID;

public record AuditDto(
    UUID id, String action, String resource, String resourceId, String actor, Instant createdAt) {}
