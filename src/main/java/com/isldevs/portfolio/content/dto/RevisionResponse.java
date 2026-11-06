package com.isldevs.portfolio.content.dto;

import java.time.Instant;
import java.util.UUID;

public record RevisionResponse(
    UUID id, Instant createdAt, String actor, ContentResponse snapshot) {}
