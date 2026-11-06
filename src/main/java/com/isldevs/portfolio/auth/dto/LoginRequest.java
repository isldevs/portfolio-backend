package com.isldevs.portfolio.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginRequest(
    @NotBlank @Size(max = 120) String username, @NotBlank @Size(max = 256) String password) {}
