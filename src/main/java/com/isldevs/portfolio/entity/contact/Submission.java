package com.isldevs.portfolio.entity.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Submission(@NotBlank @Size(max = 100) String name,
                         @NotBlank @Email @Size(max = 254) String email,
                         @NotBlank @Size(max = 200) String subject,
                         @NotBlank @Size(min = 10, max = 10000) String message,
                         @Size(max = 200) String website) {
}
