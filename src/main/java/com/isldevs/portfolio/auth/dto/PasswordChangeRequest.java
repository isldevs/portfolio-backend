package com.isldevs.portfolio.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank String currentPassword, @Size(min = 14, max = 128) String newPassword) {}
