package com.isldevs.portfolio.content.dto;

import jakarta.validation.constraints.*;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VersionRequest(Long version) {}
