package com.isldevs.portfolio.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Login(@NotBlank @Size(max = 120) String username,
                    @NotBlank @Size(max = 256) String password) {
}
