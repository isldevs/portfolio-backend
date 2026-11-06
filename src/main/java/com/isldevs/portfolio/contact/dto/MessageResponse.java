package com.isldevs.portfolio.contact.dto;

import com.isldevs.portfolio.contact.entity.Status;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    String name,
    String email,
    String subject,
    String message,
    Status status,
    long version,
    OffsetDateTime createdAt) {}
