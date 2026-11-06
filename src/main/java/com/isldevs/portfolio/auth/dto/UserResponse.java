package com.isldevs.portfolio.auth.dto;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserResponse(String username, String role) {}
