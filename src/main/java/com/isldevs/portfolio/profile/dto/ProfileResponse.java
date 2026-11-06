package com.isldevs.portfolio.profile.dto;

import java.util.UUID;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProfileResponse(
    UUID id,
    Long version,
    String fullName,
    String professionalTitle,
    String headline,
    String shortBio,
    String fullBio,
    String location,
    String specialization,
    String availability,
    String githubUrl,
    String linkedinUrl,
    String publicEmail,
    String portraitUrl,
    String resumeUrl,
    String seoTitle,
    String seoDescription) {}
