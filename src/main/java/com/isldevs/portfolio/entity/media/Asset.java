package com.isldevs.portfolio.entity.media;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Asset(UUID id,
                    String url,
                    String originalName,
                    String contentType,
                    long size,
                    Visibility visibility,
                    OffsetDateTime createdAt) {
}
