package com.isldevs.portfolio.contact.dto;

import java.util.UUID;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SubmissionResponse(UUID id, String message) {}
