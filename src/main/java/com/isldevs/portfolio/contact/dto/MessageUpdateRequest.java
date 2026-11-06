package com.isldevs.portfolio.contact.dto;

import com.isldevs.portfolio.contact.entity.Status;
import jakarta.validation.constraints.*;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MessageUpdateRequest(@NotNull Status status, @NotNull @Min(0) Long version) {}
