package com.isldevs.portfolio.entity.contact;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Message(
        UUID id,
        String name,
        String email,
        String subject,
        String message,
        Status status,
        long version,
        OffsetDateTime createdAt) {
}
