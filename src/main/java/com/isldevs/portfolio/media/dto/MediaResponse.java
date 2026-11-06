package com.isldevs.portfolio.media.dto;

import com.isldevs.portfolio.media.entity.Visibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MediaResponse(
    UUID id,
    String url,
    String originalName,
    String contentType,
    long size,
    Visibility visibility,
    OffsetDateTime createdAt) {}
