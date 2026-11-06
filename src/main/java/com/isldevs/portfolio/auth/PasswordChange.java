package com.isldevs.portfolio.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChange(@NotBlank String currentPassword,
                             @Size(min = 14, max = 128) String newPassword) {
}
