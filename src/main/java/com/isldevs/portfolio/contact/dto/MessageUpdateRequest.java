package com.isldevs.portfolio.contact.dto;

import com.isldevs.portfolio.contact.entity.Status;
import jakarta.validation.constraints.*;

public record MessageUpdateRequest(@NotNull Status status, @NotNull @Min(0) Long version) {}
